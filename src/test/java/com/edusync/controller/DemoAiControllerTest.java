package com.edusync.controller;

import com.edusync.dto.request.ExerciseGenerationRequest;
import com.edusync.dto.response.GeneratedExerciseResponse;
import com.edusync.dto.response.GeneratedInsightResponse;
import com.edusync.dto.response.GeneratedLessonResponse;
import com.edusync.exception.AiIntegrationException;
import com.edusync.exception.DemoRateLimitException;
import com.edusync.repository.UserRepository;
import com.edusync.security.CustomUserDetailsService;
import com.edusync.security.JwtAuthenticationEntryPoint;
import com.edusync.security.JwtUtil;
import com.edusync.service.DemoRateLimiter;
import com.edusync.service.GeminiIntegrationService;
import com.edusync.config.SecurityConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.nullable;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
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
        "demo.ai.enabled=true",
        "demo.ai.per-ip-limit=5",
        "demo.ai.window-hours=24",
        "demo.ai.daily-cap=200",
        "edusync.cors.allowed-origins=https://edusync-web-lac.vercel.app,https://edusync-demo.vercel.app",
        "edusync.jwt.secret=MDEyMzQ1Njc4OWFiY2RlZjAxMjM0NTY3ODlhYmNkZWY=",
        "edusync.jwt.expiration-ms=3600000",
        "edusync.jwt.issuer=edusync",
        "DB_PASSWORD=test",
        "JWT_SECRET=MDEyMzQ1Njc4OWFiY2RlZjAxMjM0NTY3ODlhYmNkZWY=",
        "GEMINI_API_KEY=test"
})
class DemoAiControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private GeminiIntegrationService geminiIntegrationService;

    @MockBean
    private UserRepository userRepository;

    @BeforeEach
    void stubGemini() {
        lenient().when(geminiIntegrationService.generateLesson(anyString(), anyString(), anyString(), nullable(String.class)))
                .thenReturn(new GeneratedLessonResponse("aula gerada"));
        lenient().when(geminiIntegrationService.generateExercises(any()))
                .thenReturn(sampleExercises());
        lenient().when(geminiIntegrationService.generateInsight(anyString(), nullable(String.class), nullable(String.class)))
                .thenReturn(new GeneratedInsightResponse("análise", List.of("a", "b", "c", "d")));
    }

    @Test
    void lessonExercisesAndInsightWorkWithoutAToken() throws Exception {
        mockMvc.perform(demo(post("/api/demo/ia/lesson"), "203.0.113.10", """
                        {"studentName":"Ana","learningLevel":"INICIANTE","topic":"verbos","observations":""}
                        """))
                .andExpect(status().isOk())
                .andExpect(header().string(DemoAiController.REMAINING_HEADER, "4"))
                .andExpect(jsonPath("$.generatedContent").value("aula gerada"));

        mockMvc.perform(demo(post("/api/demo/ia/exercises"), "203.0.113.11", """
                        {"assunto":"frações","nivelAluno":"iniciante","tipoExercicio":"quiz"}
                        """))
                .andExpect(status().isOk())
                .andExpect(header().string(DemoAiController.REMAINING_HEADER, "4"))
                .andExpect(jsonPath("$.tema").value("frações"))
                .andExpect(jsonPath("$.questoes.length()").value(3));

        mockMvc.perform(demo(post("/api/demo/ia/insight"), "203.0.113.12", """
                        {"studentName":"Bia","recentThemes":"frações","performanceSummary":"média 80"}
                        """))
                .andExpect(status().isOk())
                .andExpect(header().string(DemoAiController.REMAINING_HEADER, "4"))
                .andExpect(jsonPath("$.analysis").value("análise"))
                .andExpect(jsonPath("$.recommendations.length()").value(4));
    }

    @Test
    void weirdAuthorizationHeaderStillReachesThePublicDemo() throws Exception {
        mockMvc.perform(demo(post("/api/demo/ia/exercises"), "203.0.113.13", """
                        {"assunto":"frações","nivelAluno":"iniciante","tipoExercicio":"quiz"}
                        """)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer isto-nao-e-um-jwt"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.questoes.length()").value(3));
    }

    @Test
    void pingReadsQuotaAndDoesNotReserveIt() throws Exception {
        mockMvc.perform(demo(get("/api/demo/ping"), "203.0.113.14", null))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ok"))
                .andExpect(jsonPath("$.aiEnabled").value(true))
                .andExpect(jsonPath("$.remaining").value(5));

        mockMvc.perform(demo(get("/api/demo/ping"), "203.0.113.14", null))
                .andExpect(jsonPath("$.remaining").value(5));

        mockMvc.perform(demo(post("/api/demo/ia/lesson"), "203.0.113.14", """
                        {"studentName":"Ana","learningLevel":"INICIANTE","topic":"verbos"}
                        """))
                .andExpect(status().isOk())
                .andExpect(header().string(DemoAiController.REMAINING_HEADER, "4"));

        mockMvc.perform(demo(get("/api/demo/ping"), "203.0.113.14", null))
                .andExpect(jsonPath("$.remaining").value(4));
    }

    @Test
    void freeTextIsSanitizedBeforeItReachesGemini() throws Exception {
        mockMvc.perform(demo(post("/api/demo/ia/lesson"), "203.0.113.15", """
                        {"studentName":" Ana ","learningLevel":" INICIANTE ","topic":"Verbos\\nfrasais","observations":"linha 1\\nlinha 2"}
                        """))
                .andExpect(status().isOk());

        verify(geminiIntegrationService).generateLesson("Ana", "INICIANTE", "Verbos frasais", "linha 1 linha 2");
    }

    @Test
    void exercisesBodyIsMappedToTheExistingRequest() throws Exception {
        mockMvc.perform(demo(post("/api/demo/ia/exercises"), "203.0.113.16", """
                        {"assunto":"  frações\\n","nivelAluno":"iniciante","tipoExercicio":"quiz"}
                        """))
                .andExpect(status().isOk());

        verify(geminiIntegrationService).generateExercises(
                new ExerciseGenerationRequest("frações", "iniciante", "quiz"));
    }

    @Test
    void oversizedFieldReturns400WithoutCallingGemini() throws Exception {
        String topic = "a".repeat(201);
        String json = """
                {"studentName":"Ana","learningLevel":"INICIANTE","topic":"%s"}
                """.formatted(topic);

        mockMvc.perform(demo(post("/api/demo/ia/lesson"), "203.0.113.17", json))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.fieldErrors.topic").exists());

        verify(geminiIntegrationService, never()).generateLesson(anyString(), anyString(), eq(topic), nullable(String.class));
        mockMvc.perform(demo(get("/api/demo/ping"), "203.0.113.17", null))
                .andExpect(jsonPath("$.remaining").value(5));
    }

    @Test
    void sixthCallFromTheSameIpReturns429WithRetryAfter() throws Exception {
        String body = """
                {"assunto":"frações","nivelAluno":"iniciante","tipoExercicio":"quiz"}
                """;
        for (int i = 0; i < 5; i++) {
            mockMvc.perform(demo(post("/api/demo/ia/exercises"), "203.0.113.60", body))
                    .andExpect(status().isOk())
                    .andExpect(header().string(DemoAiController.REMAINING_HEADER, Integer.toString(4 - i)));
        }

        mockMvc.perform(demo(post("/api/demo/ia/exercises"), "203.0.113.60", body))
                .andExpect(status().isTooManyRequests())
                .andExpect(header().string(HttpHeaders.RETRY_AFTER, org.hamcrest.Matchers.matchesPattern("[1-9][0-9]*")))
                .andExpect(jsonPath("$.status").value(429))
                .andExpect(jsonPath("$.message").value(DemoRateLimitException.MESSAGE));
    }

    @Test
    void geminiFailureConsumesTheSlotAndOmitsTheRemainingHeader() throws Exception {
        when(geminiIntegrationService.generateInsight(eq("Caio"), nullable(String.class), nullable(String.class)))
                .thenThrow(new AiIntegrationException("falha de teste"));

        mockMvc.perform(demo(post("/api/demo/ia/insight"), "203.0.113.18", """
                        {"studentName":"Caio"}
                        """))
                .andExpect(status().isBadGateway())
                .andExpect(header().doesNotExist(DemoAiController.REMAINING_HEADER))
                .andExpect(jsonPath("$.message").value("falha de teste"));

        mockMvc.perform(demo(get("/api/demo/ping"), "203.0.113.18", null))
                .andExpect(jsonPath("$.remaining").value(4));
    }

    @Test
    void studentsRouteStaysProtected() throws Exception {
        mockMvc.perform(get("/api/students"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void preflightAcceptsConfiguredOriginsAndRejectsTheRest() throws Exception {
        mockMvc.perform(options("/api/demo/ia/exercises")
                        .header(HttpHeaders.ORIGIN, "https://edusync-demo.vercel.app")
                        .header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "POST")
                        .header(HttpHeaders.ACCESS_CONTROL_REQUEST_HEADERS, "content-type"))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, "https://edusync-demo.vercel.app"))
                .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_EXPOSE_HEADERS,
                        org.hamcrest.Matchers.containsString("X-Demo-AI-Remaining")))
                .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_EXPOSE_HEADERS,
                        org.hamcrest.Matchers.containsString("Retry-After")));

        mockMvc.perform(options("/api/demo/ping")
                        .header(HttpHeaders.ORIGIN, "https://edusync-web-lac.vercel.app")
                        .header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "GET"))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, "https://edusync-web-lac.vercel.app"));

        mockMvc.perform(options("/api/demo/ia/lesson")
                        .header(HttpHeaders.ORIGIN, "https://evil.example")
                        .header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "POST"))
                .andExpect(status().isForbidden())
                .andExpect(header().doesNotExist(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN));
    }

    private static GeneratedExerciseResponse sampleExercises() {
        var question = new GeneratedExerciseResponse.Questao(
                "Quanto é 2+2?",
                List.of("A) 3", "B) 4", "C) 5", "D) 6"),
                "B) 4",
                "Soma simples.");
        return new GeneratedExerciseResponse("frações", "iniciante", List.of(question, question, question));
    }

    private static MockHttpServletRequestBuilder demo(MockHttpServletRequestBuilder builder, String clientIp, String json) {
        builder.with(request -> {
            request.setRemoteAddr("10.8.0.4");
            request.addHeader("X-Forwarded-For", clientIp);
            return request;
        });
        if (json != null) {
            builder.contentType(MediaType.APPLICATION_JSON).content(json);
        }
        return builder;
    }
}
