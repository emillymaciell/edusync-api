package com.edusync.config;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class SecurityConfigOriginsTest {

    @Test
    void blankCsvKeepsTheCurrentFrontendOrigin() {
        assertEquals(List.of(SecurityConfig.DEFAULT_ORIGIN), SecurityConfig.parseAllowedOrigins(null));
        assertEquals(List.of(SecurityConfig.DEFAULT_ORIGIN), SecurityConfig.parseAllowedOrigins("  ,  "));
    }

    @Test
    void csvTrimsOriginsAndDropsEmptyEntries() {
        assertEquals(
                List.of("https://edusync-web-lac.vercel.app", "https://edusync-demo.vercel.app"),
                SecurityConfig.parseAllowedOrigins(
                        " https://edusync-web-lac.vercel.app, https://edusync-demo.vercel.app, "));
    }
}
