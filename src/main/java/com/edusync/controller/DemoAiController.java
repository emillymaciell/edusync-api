package com.edusync.controller;

import com.edusync.dto.demo.DemoExercisesRequest;
import com.edusync.dto.demo.DemoInsightRequest;
import com.edusync.dto.demo.DemoLessonRequest;
import com.edusync.dto.demo.DemoPingResponse;
import com.edusync.dto.request.ExerciseGenerationRequest;
import com.edusync.exception.ApiError;
import com.edusync.exception.DemoRateLimitException;
import com.edusync.service.DemoRateLimiter;
import com.edusync.service.GeminiIntegrationService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.function.Supplier;

/**
 * Demonstração pública de IA. Não injeta repositório e não persiste nada.
 *
 * <p>Contrato de cota, para o front não divergir do servidor:
 * <ul>
 *   <li>A autoridade é este back-end. O limite é por IP (janela deslizante),
 *       com um teto global diário. Não é por visitante: apagar o sessionStorage
 *       não libera geração nova.</li>
 *   <li>{@code GET /api/demo/ping} apenas consulta a cota efetiva daquele IP.
 *       Não reserva e não consome.</li>
 *   <li>Cada {@code POST /api/demo/ia/**} admitido consome uma geração antes
 *       de chamar o Gemini. Falha da IA não devolve a cota: isso impede rajada
 *       contra a cota do provedor e mantém o teto atômico sob concorrência.</li>
 *   <li>Resposta de sucesso traz {@code X-Demo-AI-Remaining} já descontada.
 *       O front deve substituir o número exibido por esse valor. Fallback
 *       (qualquer status fora de 2xx) não é geração bem-sucedida no contador
 *       visual; depois de um erro, reler {@code /ping} para sincronizar,
 *       porque o 502 não repete o header de cota.</li>
 *   <li>Com {@code demo.ai.enabled=false}, {@code /ia/**} responde 503 sem
 *       chamar o Gemini e sem consumir cota. O ping segue 200 com
 *       {@code aiEnabled=false}.</li>
 * </ul>
 */
@Slf4j
@RestController
@RequestMapping("/api/demo")
@RequiredArgsConstructor
public class DemoAiController {

    public static final String REMAINING_HEADER = "X-Demo-AI-Remaining";
    static final String DISABLED_MESSAGE = "A geração por IA da demonstração está desativada.";

    private final GeminiIntegrationService geminiIntegrationService;
    private final DemoRateLimiter demoRateLimiter;

    @Value("${demo.ai.enabled:true}")
    private boolean aiEnabled;

    /**
     * Acorda a instância e informa a cota efetiva do IP.
     * Não chama {@code tryAcquire}.
     */
    @GetMapping("/ping")
    public DemoPingResponse ping(HttpServletRequest http) {
        long started = System.nanoTime();
        String ip = demoRateLimiter.resolveClientIp(http);
        int remaining = demoRateLimiter.remaining(ip);
        log.info("[DemoAI] rota=GET /api/demo/ping resultado=ok duracaoMs={} aiEnabled={} ipHash={}",
                elapsedMs(started), aiEnabled, DemoRateLimiter.shortHash(ip));
        return new DemoPingResponse("ok", aiEnabled, remaining);
    }

    @PostMapping("/ia/lesson")
    public ResponseEntity<?> lesson(@Valid @RequestBody DemoLessonRequest body, HttpServletRequest http) {
        return guarded(http, "POST /api/demo/ia/lesson", () -> geminiIntegrationService.generateLesson(
                sanitize(body.studentName()),
                sanitize(body.learningLevel()),
                sanitize(body.topic()),
                sanitize(body.observations())));
    }

    @PostMapping("/ia/exercises")
    public ResponseEntity<?> exercises(@Valid @RequestBody DemoExercisesRequest body, HttpServletRequest http) {
        return guarded(http, "POST /api/demo/ia/exercises", () -> {
            var request = new ExerciseGenerationRequest(
                    sanitize(body.assunto()),
                    sanitize(body.nivelAluno()),
                    sanitize(body.tipoExercicio()));
            return geminiIntegrationService.generateExercises(request);
        });
    }

    @PostMapping("/ia/insight")
    public ResponseEntity<?> insight(@Valid @RequestBody DemoInsightRequest body, HttpServletRequest http) {
        return guarded(http, "POST /api/demo/ia/insight", () -> geminiIntegrationService.generateInsight(
                sanitize(body.studentName()),
                sanitize(body.recentThemes()),
                sanitize(body.performanceSummary())));
    }

    private <T> ResponseEntity<?> guarded(HttpServletRequest http, String route, Supplier<T> action) {
        long started = System.nanoTime();
        String outcome = "erro";
        String ip = demoRateLimiter.resolveClientIp(http);
        try {
            if (!aiEnabled) {
                outcome = "desativado";
                return unavailable();
            }
            if (!demoRateLimiter.tryAcquire(ip)) {
                outcome = "limite";
                throw new DemoRateLimitException(demoRateLimiter.retryAfterSeconds(ip));
            }
            T body = action.get();
            outcome = "ok";
            return ResponseEntity.ok()
                    .header(REMAINING_HEADER, Integer.toString(demoRateLimiter.remaining(ip)))
                    .body(body);
        } finally {
            log.info("[DemoAI] rota={} resultado={} duracaoMs={} ipHash={}",
                    route, outcome, elapsedMs(started), DemoRateLimiter.shortHash(ip));
        }
    }

    private static ResponseEntity<ApiError> unavailable() {
        HttpStatus status = HttpStatus.SERVICE_UNAVAILABLE;
        return ResponseEntity.status(status)
                .body(ApiError.of(status.value(), status.getReasonPhrase(), DISABLED_MESSAGE));
    }

    /** Tira quebras de linha e controles, porque os prompts interpolam o texto entre aspas. */
    private static String sanitize(String value) {
        if (value == null) {
            return null;
        }
        return value.trim().replaceAll("[\\p{Cc}\\p{Zl}\\p{Zp}]", " ");
    }

    private static long elapsedMs(long startedNanos) {
        return Math.max(0L, (System.nanoTime() - startedNanos) / 1_000_000L);
    }
}
