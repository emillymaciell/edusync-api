package com.edusync.controller;

import com.edusync.repository.UserRepository;
import com.edusync.security.CustomUserDetailsService;
import com.edusync.security.JwtAuthenticationEntryPoint;
import com.edusync.security.JwtUtil;
import com.edusync.service.DemoRateLimiter;
import com.edusync.service.GeminiIntegrationService;
import com.edusync.config.SecurityConfig;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.nullable;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = DemoAiController.class)
@AutoConfigureMockMvc
@Import({
        SecurityConfig.class,
        JwtAuthenticationEntryPoint.class,
        JwtUtil.class,
        CustomUserDetailsService.class,
        DemoRateLimiter.class
})
@TestPropertySource(properties = {
        "demo.ai.enabled=false",
        "demo.ai.per-ip-limit=5",
        "demo.ai.window-hours=24",
        "demo.ai.daily-cap=200",
        "edusync.cors.allowed-origins=https://edusync-web-lac.vercel.app",
        "edusync.jwt.secret=MDEyMzQ1Njc4OWFiY2RlZjAxMjM0NTY3ODlhYmNkZWY=",
        "edusync.jwt.expiration-ms=3600000",
        "edusync.jwt.issuer=edusync",
        "DB_PASSWORD=test",
        "JWT_SECRET=MDEyMzQ1Njc4OWFiY2RlZjAxMjM0NTY3ODlhYmNkZWY=",
        "GEMINI_API_KEY=test"
})
class DemoAiDisabledControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private GeminiIntegrationService geminiIntegrationService;

    @MockBean
    private UserRepository userRepository;

    @Test
    void disabledAiReturns503AndPingReportsItWithoutConsumingQuota() throws Exception {
        mockMvc.perform(post("/api/demo/ia/exercises")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"assunto":"frações","nivelAluno":"iniciante","tipoExercicio":"quiz"}
                                """))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.status").value(503))
                .andExpect(jsonPath("$.message").value(DemoAiController.DISABLED_MESSAGE));

        mockMvc.perform(post("/api/demo/ia/lesson")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"studentName":"Ana","learningLevel":"INICIANTE","topic":"verbos"}
                                """))
                .andExpect(status().isServiceUnavailable());

        mockMvc.perform(post("/api/demo/ia/insight")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"studentName":"Bia"}
                                """))
                .andExpect(status().isServiceUnavailable());

        verify(geminiIntegrationService, never()).generateExercises(any());
        verify(geminiIntegrationService, never()).generateLesson(
                nullable(String.class), nullable(String.class), nullable(String.class), nullable(String.class));
        verify(geminiIntegrationService, never()).generateInsight(
                nullable(String.class), nullable(String.class), nullable(String.class));

        mockMvc.perform(get("/api/demo/ping"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ok"))
                .andExpect(jsonPath("$.aiEnabled").value(false))
                .andExpect(jsonPath("$.remaining").value(5));

        mockMvc.perform(get("/api/students"))
                .andExpect(status().isUnauthorized());
    }
}
