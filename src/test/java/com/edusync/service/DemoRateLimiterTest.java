package com.edusync.service;

import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DemoRateLimiterTest {

    private static final Instant NOON_UTC = Instant.parse("2026-06-01T15:00:00Z");

    @Test
    void allowsRequestsInsideThePerIpLimitAndThenRejects() {
        DemoRateLimiter limiter = limiter(5, 24, 200, clockAt(NOON_UTC));

        for (int i = 0; i < 5; i++) {
            assertTrue(limiter.tryAcquire("203.0.113.10"));
            assertEquals(4 - i, limiter.remaining("203.0.113.10"));
        }
        assertFalse(limiter.tryAcquire("203.0.113.10"));
        assertEquals(0, limiter.remaining("203.0.113.10"));
        assertEquals(24 * 60 * 60, limiter.retryAfterSeconds("203.0.113.10"));
        assertEquals(5, limiter.remaining("203.0.113.11"));
    }

    @Test
    void dailyCapBlocksOtherIpsUntilSaoPauloMidnight() {
        MutableClock clock = clockAt(Instant.parse("2026-06-01T04:00:00Z"));
        DemoRateLimiter limiter = limiter(10, 24, 2, clock);

        assertTrue(limiter.tryAcquire("203.0.113.1"));
        assertTrue(limiter.tryAcquire("203.0.113.2"));
        assertFalse(limiter.tryAcquire("203.0.113.3"));
        assertEquals(0, limiter.remaining("203.0.113.1"));
        assertEquals(0, limiter.remaining("198.51.100.8"));
        assertEquals(23 * 60 * 60, limiter.retryAfterSeconds("203.0.113.3"));

        clock.set(Instant.parse("2026-06-02T02:59:59Z"));
        assertFalse(limiter.tryAcquire("203.0.113.3"));

        clock.set(Instant.parse("2026-06-02T03:00:00Z"));
        assertEquals(2, limiter.remaining("203.0.113.1"));
        assertTrue(limiter.tryAcquire("203.0.113.3"));
        assertEquals(1, limiter.remaining("203.0.113.4"));
    }

    @Test
    void slidingWindowExpiresWithoutErasingNewerHits() {
        MutableClock clock = clockAt(NOON_UTC);
        DemoRateLimiter limiter = limiter(2, 24, 100, clock);

        assertTrue(limiter.tryAcquire("203.0.113.20"));
        clock.set(NOON_UTC.plus(Duration.ofHours(1)));
        assertTrue(limiter.tryAcquire("203.0.113.20"));
        assertFalse(limiter.tryAcquire("203.0.113.20"));

        clock.set(NOON_UTC.plus(Duration.ofHours(24)));
        assertTrue(limiter.tryAcquire("203.0.113.20"));
        assertFalse(limiter.tryAcquire("203.0.113.20"));

        clock.set(NOON_UTC.plus(Duration.ofHours(25)));
        assertTrue(limiter.tryAcquire("203.0.113.20"));
        assertEquals(0, limiter.remaining("203.0.113.20"));
    }

    @Test
    void pingStyleReadDoesNotConsumeQuota() {
        DemoRateLimiter limiter = limiter(5, 24, 200, clockAt(NOON_UTC));

        assertEquals(5, limiter.remaining("203.0.113.30"));
        assertEquals(5, limiter.remaining("203.0.113.30"));
        assertTrue(limiter.tryAcquire("203.0.113.30"));
        assertEquals(4, limiter.remaining("203.0.113.30"));
    }

    @Test
    void clientIpComesFromForwardedHeaderOnlyBehindATrustedProxy() {
        DemoRateLimiter limiter = limiter(5, 24, 200, Clock.systemUTC());

        HttpServletRequest proxied = request("10.0.0.8", "203.0.113.10, 10.0.0.8");
        assertEquals("203.0.113.10", limiter.resolveClientIp(proxied));

        HttpServletRequest spaced = request("192.168.1.2", " 2001:DB8::10 , 192.168.1.2");
        assertEquals("2001:db8::10", limiter.resolveClientIp(spaced));

        HttpServletRequest spoofed = request("198.51.100.20", "203.0.113.10");
        assertEquals("198.51.100.20", limiter.resolveClientIp(spoofed));

        HttpServletRequest direct = request("203.0.113.7", null);
        assertEquals("203.0.113.7", limiter.resolveClientIp(direct));

        HttpServletRequest garbage = request("10.1.1.1", "unknown, 203.0.113.10");
        assertEquals("10.1.1.1", limiter.resolveClientIp(garbage));

        String client = limiter.resolveClientIp(proxied);
        assertTrue(limiter.tryAcquire(client));
        assertEquals(4, limiter.remaining("203.0.113.10"));
        assertEquals(5, limiter.remaining("10.0.0.8"));
    }

    @Test
    void purgeDropsOnlyExpiredClients() {
        MutableClock clock = clockAt(NOON_UTC);
        DemoRateLimiter limiter = limiter(5, 24, 100, clock);
        assertTrue(limiter.tryAcquire("203.0.113.40"));
        clock.set(NOON_UTC.plus(Duration.ofHours(23)));
        assertTrue(limiter.tryAcquire("203.0.113.41"));

        clock.set(NOON_UTC.plus(Duration.ofHours(24)).plusSeconds(1));
        limiter.purgeExpired();

        assertEquals(1, limiter.trackedClients());
        assertEquals(5, limiter.remaining("203.0.113.40"));
        assertEquals(4, limiter.remaining("203.0.113.41"));
    }

    @Test
    void concurrentCallsDoNotPassThePerIpLimit() throws Exception {
        DemoRateLimiter limiter = limiter(5, 24, 1_000, Clock.systemUTC());
        int admitted = hammer(limiter, 32, index -> "203.0.113.9");
        assertEquals(5, admitted);
        assertEquals(0, limiter.remaining("203.0.113.9"));
    }

    @Test
    void concurrentCallsDoNotPassTheDailyCap() throws Exception {
        DemoRateLimiter limiter = limiter(100, 24, 10, Clock.systemUTC());
        int admitted = hammer(limiter, 40, index -> "198.51.100." + ((index % 20) + 1));
        assertEquals(10, admitted);
        assertEquals(0, limiter.remaining("198.51.100.50"));
    }

    @Test
    void shortHashHidesTheIp() {
        String hash = DemoRateLimiter.shortHash("203.0.113.10");
        assertEquals(8, hash.length());
        assertTrue(hash.matches("[0-9a-f]{8}"));
        assertNotEquals("203.0.113.10", hash);
        assertEquals(hash, DemoRateLimiter.shortHash("203.0.113.10"));
    }

    private static int hammer(DemoRateLimiter limiter, int threads, java.util.function.IntFunction<String> ip)
            throws InterruptedException {
        ExecutorService pool = Executors.newFixedThreadPool(threads);
        CountDownLatch start = new CountDownLatch(1);
        CountDownLatch done = new CountDownLatch(threads);
        AtomicInteger successes = new AtomicInteger();
        try {
            for (int i = 0; i < threads; i++) {
                int index = i;
                pool.submit(() -> {
                    try {
                        if (!start.await(5, TimeUnit.SECONDS)) {
                            return;
                        }
                        if (limiter.tryAcquire(ip.apply(index))) {
                            successes.incrementAndGet();
                        }
                    } catch (InterruptedException ex) {
                        Thread.currentThread().interrupt();
                    } finally {
                        done.countDown();
                    }
                });
            }
            start.countDown();
            assertTrue(done.await(10, TimeUnit.SECONDS));
            return successes.get();
        } finally {
            pool.shutdownNow();
        }
    }

    private static DemoRateLimiter limiter(int perIp, long hours, int dailyCap, Clock clock) {
        return new DemoRateLimiter(perIp, hours, dailyCap, clock);
    }

    private static MutableClock clockAt(Instant instant) {
        return new MutableClock(instant);
    }

    private static MockHttpServletRequest request(String remoteAddr, String forwarded) {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRemoteAddr(remoteAddr);
        if (forwarded != null) {
            request.addHeader("X-Forwarded-For", forwarded);
        }
        return request;
    }

    private static final class MutableClock extends Clock {
        private Instant current;

        private MutableClock(Instant current) {
            this.current = current;
        }

        private void set(Instant instant) {
            this.current = instant;
        }

        @Override
        public ZoneId getZone() {
            return ZoneOffset.UTC;
        }

        @Override
        public Clock withZone(ZoneId zone) {
            return this;
        }

        @Override
        public Instant instant() {
            return current;
        }
    }
}
