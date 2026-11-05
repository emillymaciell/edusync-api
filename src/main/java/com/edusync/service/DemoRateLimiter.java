package com.edusync.service;

import jakarta.annotation.PostConstruct;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.net.Inet6Address;
import java.net.InetAddress;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Locale;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;

/**
 * Cota da demonstração de IA, só em memória.
 *
 * <p><b>Por IP, não por visitante.</b> O limite de {@code demo.ai.per-ip-limit}
 * gerações vale para o IP do cliente numa janela deslizante de
 * {@code demo.ai.window-hours}. Limpar sessionStorage, cookie ou qualquer id
 * enviado pelo navegador não devolve cota: esses identificadores não são lidos.
 *
 * <p><b>Teto global.</b> {@code demo.ai.daily-cap} conta todas as gerações
 * admitidas e zera à meia-noite em {@code America/Sao_Paulo}. A cota efetiva
 * de um IP é o mínimo entre o que ainda cabe na janela dele e o que ainda
 * cabe no teto do dia.
 *
 * <p><b>Reinício.</b> O estado vive no heap. Cold start ou restart do Render
 * zera o contador por IP e o teto global. O teto diário é, portanto,
 * aproximado entre processos: não sobrevive a reinício porque a demo não
 * grava em banco. Dentro de um processo, as duas cotas são atômicas
 * (um único lock) e requisições simultâneas não ultrapassam os limites.
 *
 * <p><b>Consumo.</b> {@link #tryAcquire(String)} reserva a geração na hora.
 * Quem chama deve fazê-lo antes do Gemini: se a IA falhar depois, a cota
 * não é devolvida. {@link #remaining(String)} só consulta.
 *
 * <p><b>IP.</b> {@code X-Forwarded-For} só é aceito quando o peer TCP é um
 * proxy confiável (loopback, link-local ou rede privada, como o proxy do
 * Render). Usa-se o primeiro hop, que é o cliente. Conexão direta ignora o
 * header, para um visitante não escolher o próprio IP. Nos logs, só entra
 * um hash curto.
 */
@Slf4j
@Component
public class DemoRateLimiter {

    static final ZoneId SAO_PAULO = ZoneId.of("America/Sao_Paulo");
    private static final Pattern IPV4 = Pattern.compile(
            "^((25[0-5]|2[0-4]\\d|[01]?\\d\\d?)\\.){3}(25[0-5]|2[0-4]\\d|[01]?\\d\\d?)$");

    private final int perIpLimit;
    private final Duration window;
    private final int dailyCap;
    private final Clock clock;
    private final ConcurrentHashMap<String, Deque<Instant>> hitsByIp = new ConcurrentHashMap<>();
    private final Object globalLock = new Object();
    private int globalCount;
    private LocalDate globalDay;

    @Autowired
    public DemoRateLimiter(
            @Value("${demo.ai.per-ip-limit:5}") int perIpLimit,
            @Value("${demo.ai.window-hours:24}") long windowHours,
            @Value("${demo.ai.daily-cap:200}") int dailyCap,
            ObjectProvider<Clock> clockProvider) {
        this(perIpLimit, windowHours, dailyCap, clockProvider.getIfAvailable(Clock::systemUTC));
    }

    /** Construtor direto para testes, com relógio controlável. */
    public DemoRateLimiter(int perIpLimit, long windowHours, int dailyCap, Clock clock) {
        if (perIpLimit < 1 || windowHours < 1 || dailyCap < 1) {
            throw new IllegalArgumentException(
                    "demo.ai.per-ip-limit, demo.ai.window-hours e demo.ai.daily-cap devem ser maiores que zero.");
        }
        if (clock == null) {
            throw new IllegalArgumentException("Clock é obrigatório.");
        }
        this.perIpLimit = perIpLimit;
        this.window = Duration.ofHours(windowHours);
        this.dailyCap = dailyCap;
        this.clock = clock;
    }

    @PostConstruct
    void logLimits() {
        log.info("[DemoAI] Limite por IP={} janelaHoras={} tetoDiario={} fuso=America/Sao_Paulo. "
                        + "Contadores só em memória: um restart zera a cota, inclusive o teto global.",
                perIpLimit, window.toHours(), dailyCap);
    }

    /**
     * Reserva uma geração para o IP se ainda couber na janela dele e no teto do dia.
     * A reserva não é desfeita se a chamada de IA posterior falhar.
     *
     * @return {@code true} quando a geração foi consumida
     */
    public boolean tryAcquire(String ip) {
        String key = normalize(ip);
        Instant now = clock.instant();
        synchronized (globalLock) {
            rollDay(now);
            Deque<Instant> deque = hitsByIp.computeIfAbsent(key, ignored -> new ArrayDeque<>());
            synchronized (deque) {
                evict(deque, now);
                if (deque.size() >= perIpLimit || globalCount >= dailyCap) {
                    if (deque.isEmpty()) {
                        hitsByIp.remove(key, deque);
                    }
                    return false;
                }
                deque.addLast(now);
                globalCount++;
                return true;
            }
        }
    }

    /**
     * Cota ainda disponível para o IP, sem reservar.
     * É o mínimo entre as vagas da janela e as vagas do teto global de hoje.
     */
    public int remaining(String ip) {
        String key = normalize(ip);
        Instant now = clock.instant();
        synchronized (globalLock) {
            rollDay(now);
            int ipUsed = 0;
            Deque<Instant> deque = hitsByIp.get(key);
            if (deque != null) {
                synchronized (deque) {
                    evict(deque, now);
                    ipUsed = deque.size();
                    if (deque.isEmpty()) {
                        hitsByIp.remove(key, deque);
                    }
                }
            }
            int ipRemaining = Math.max(0, perIpLimit - ipUsed);
            int globalRemaining = Math.max(0, dailyCap - globalCount);
            return Math.min(ipRemaining, globalRemaining);
        }
    }

    /**
     * Segundos até a próxima vaga, considerando o que realmente bloqueou.
     * Se IP e teto global estiverem cheios, espera o que liberar por último,
     * porque os dois precisam ter vaga ao mesmo tempo.
     */
    public int retryAfterSeconds(String ip) {
        String key = normalize(ip);
        Instant now = clock.instant();
        synchronized (globalLock) {
            rollDay(now);
            int ipWait = 0;
            Deque<Instant> deque = hitsByIp.get(key);
            if (deque != null) {
                synchronized (deque) {
                    evict(deque, now);
                    if (deque.size() >= perIpLimit) {
                        ipWait = secondsUntil(now, earliest(deque).plus(window));
                    }
                }
            }
            int globalWait = globalCount >= dailyCap ? secondsUntilMidnight(now) : 0;
            if (ipWait > 0 && globalWait > 0) {
                return Math.max(ipWait, globalWait);
            }
            if (ipWait > 0) {
                return ipWait;
            }
            if (globalWait > 0) {
                return globalWait;
            }
            return 1;
        }
    }

    /**
     * IP do cliente. O primeiro valor de {@code X-Forwarded-For} só vale quando
     * {@code remoteAddr} é um proxy de rede privada ou loopback. Caso contrário
     * usa-se o peer da conexão, e o header é ignorado.
     */
    public String resolveClientIp(HttpServletRequest request) {
        String remote = request.getRemoteAddr();
        String fallback = (remote == null || remote.isBlank()) ? "unknown" : remote.trim();
        if (!isTrustedProxy(fallback)) {
            return normalize(fallback);
        }
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded == null || forwarded.isBlank()) {
            return normalize(fallback);
        }
        String first = forwarded.split(",", 2)[0].trim();
        if (!isLiteralIp(first)) {
            return normalize(fallback);
        }
        return normalize(first);
    }

    /** Oito primeiros caracteres hexadecimais do SHA-256. Nunca registre o IP cru. */
    public static String shortHash(String ip) {
        String value = (ip == null || ip.isBlank()) ? "unknown" : ip;
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest(value.getBytes(StandardCharsets.UTF_8));
            StringBuilder hex = new StringBuilder(16);
            for (int i = 0; i < 4; i++) {
                int unsigned = digest[i] & 0xff;
                hex.append(Character.forDigit(unsigned >>> 4, 16));
                hex.append(Character.forDigit(unsigned & 0x0f, 16));
            }
            return hex.toString();
        } catch (NoSuchAlgorithmException ex) {
            return "00000000";
        }
    }

    /** Remove entradas cuja janela já passou. Também vira o dia global se preciso. */
    @Scheduled(fixedDelay = 15 * 60 * 1000L, initialDelay = 15 * 60 * 1000L)
    public void purgeExpired() {
        Instant now = clock.instant();
        synchronized (globalLock) {
            rollDay(now);
            hitsByIp.entrySet().removeIf(entry -> {
                Deque<Instant> deque = entry.getValue();
                synchronized (deque) {
                    evict(deque, now);
                    return deque.isEmpty();
                }
            });
        }
    }

    /** Visível para o teste da limpeza. */
    int trackedClients() {
        synchronized (globalLock) {
            return hitsByIp.size();
        }
    }

    private void rollDay(Instant now) {
        LocalDate today = LocalDate.ofInstant(now, SAO_PAULO);
        if (!today.equals(globalDay)) {
            globalDay = today;
            globalCount = 0;
        }
    }

    private void evict(Deque<Instant> deque, Instant now) {
        Instant cutoff = now.minus(window);
        deque.removeIf(timestamp -> !timestamp.isAfter(cutoff));
    }

    private int secondsUntilMidnight(Instant now) {
        ZonedDateTime zoned = now.atZone(SAO_PAULO);
        Instant nextMidnight = zoned.toLocalDate().plusDays(1).atStartOfDay(SAO_PAULO).toInstant();
        return secondsUntil(now, nextMidnight);
    }

    private static int secondsUntil(Instant now, Instant deadline) {
        long millis = Duration.between(now, deadline).toMillis();
        if (millis <= 0) {
            return 1;
        }
        long seconds = (millis + 999) / 1000;
        return (int) Math.min(Integer.MAX_VALUE, seconds);
    }

    private static Instant earliest(Deque<Instant> deque) {
        Instant min = null;
        for (Instant timestamp : deque) {
            if (min == null || timestamp.isBefore(min)) {
                min = timestamp;
            }
        }
        if (min == null) {
            throw new IllegalStateException("Fila de acessos vazia.");
        }
        return min;
    }

    private static String normalize(String ip) {
        if (ip == null || ip.isBlank()) {
            return "unknown";
        }
        return ip.trim().toLowerCase(Locale.ROOT);
    }

    private static boolean isTrustedProxy(String remote) {
        if (remote == null || remote.isBlank()) {
            return false;
        }
        String candidate = remote.trim();
        if ("localhost".equalsIgnoreCase(candidate)) {
            return true;
        }
        if (!isLiteralIp(candidate)) {
            return false;
        }
        try {
            InetAddress address = InetAddress.getByName(candidate);
            if (address.isLoopbackAddress() || address.isLinkLocalAddress()
                    || address.isSiteLocalAddress() || address.isAnyLocalAddress()) {
                return true;
            }
            byte[] bytes = address.getAddress();
            return bytes.length == 16 && (bytes[0] & 0xfe) == 0xfc;
        } catch (Exception ex) {
            return false;
        }
    }

    private static boolean isLiteralIp(String value) {
        if (value == null || value.isEmpty() || value.length() > 45) {
            return false;
        }
        if (IPV4.matcher(value).matches()) {
            return true;
        }
        if (value.indexOf(':') < 0) {
            return false;
        }
        int colons = 0;
        for (int i = 0; i < value.length(); i++) {
            char ch = value.charAt(i);
            boolean hex = (ch >= '0' && ch <= '9')
                    || (ch >= 'a' && ch <= 'f')
                    || (ch >= 'A' && ch <= 'F');
            if (ch == ':') {
                colons++;
            } else if (!hex) {
                return false;
            }
        }
        if (colons < 2) {
            return false;
        }
        try {
            return InetAddress.getByName(value) instanceof Inet6Address;
        } catch (Exception ex) {
            return false;
        }
    }
}
