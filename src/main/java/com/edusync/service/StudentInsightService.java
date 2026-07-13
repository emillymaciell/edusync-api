package com.edusync.service;

import com.edusync.domain.enums.StudentStatus;
import com.edusync.domain.enums.TaskStatus;
import com.edusync.dto.response.GeneratedInsightResponse;
import com.edusync.dto.response.StudentInsightResponse;
import com.edusync.entity.StudentInsight;
import com.edusync.entity.StudentProfile;
import com.edusync.entity.StudentProgress;
import com.edusync.entity.Task;
import com.edusync.exception.ResourceNotFoundException;
import com.edusync.repository.StudentInsightRepository;
import com.edusync.repository.StudentProfileRepository;
import com.edusync.repository.StudentProgressRepository;
import com.edusync.repository.TaskRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Orquestra a feature "Insights IA": coleta o histórico recente do aluno,
 * aciona o {@link GeminiIntegrationService} quando necessário e aplica a
 * política de cache de 24h. Mantém o serviço de IA livre de dependências de
 * persistência (SRP / separação de responsabilidades).
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class StudentInsightService {

    private static final int INSIGHT_TTL_HOURS = 24;

    private final StudentProfileRepository studentProfileRepository;
    private final StudentProgressRepository studentProgressRepository;
    private final TaskRepository taskRepository;
    private final StudentInsightRepository studentInsightRepository;
    private final GeminiIntegrationService geminiIntegrationService;

    /**
     * Retorna o insight do aluno aplicando a regra de frescor: se o insight salvo
     * tiver menos de 24h, é reaproveitado; caso contrário (ou inexistente), um novo
     * é gerado pela IA.
     */
    @Transactional
    public StudentInsightResponse getStudentInsight(Long studentId) {
        StudentProfile student = findStudent(studentId);

        StudentInsight insight = studentInsightRepository
                .findFirstByStudentProfileIdOrderByUpdatedAtDesc(studentId)
                .orElse(null);

        if (insight != null && isFresh(insight)) {
            log.debug("[Insight] Reaproveitando insight recente do aluno {}", studentId);
            return toResponse(insight, student);
        }

        log.debug("[Insight] Gerando novo insight via IA para o aluno {}", studentId);
        StudentInsight generated = generateAndPersist(student, insight);
        return toResponse(generated, student);
    }

    /** Um insight é considerado "fresco" se foi atualizado nas últimas 24h. */
    private boolean isFresh(StudentInsight insight) {
        return insight.getUpdatedAt() != null
                && insight.getUpdatedAt().isAfter(LocalDateTime.now().minusHours(INSIGHT_TTL_HOURS));
    }

    /**
     * Coleta os dados recentes do aluno, chama a IA e faz o upsert do insight
     * (atualiza o registro existente, se houver; senão cria um novo).
     */
    private StudentInsight generateAndPersist(StudentProfile student, StudentInsight existing) {
        List<Task> recentTasks = taskRepository
                .findTop5ByStudentsIdAndStatusOrderByDueDateDesc(student.getId(), TaskStatus.CORRIGIDA);
        StudentProgress progress = studentProgressRepository.findByStudentId(student.getId()).orElse(null);

        String studentName = student.getUser().getName();
        String recentThemes = buildRecentThemes(recentTasks);
        String performanceSummary = buildPerformanceSummary(progress, recentTasks);

        GeneratedInsightResponse ai = geminiIntegrationService
                .generateInsight(studentName, recentThemes, performanceSummary);

        StudentInsight insight = existing != null ? existing : new StudentInsight();
        insight.setStudentProfile(student);
        insight.setAnalysis(ai.analysis());
        insight.setRecommendations(ai.recommendations() != null ? new ArrayList<>(ai.recommendations()) : new ArrayList<>());
        insight.setUpdatedAt(LocalDateTime.now());

        return studentInsightRepository.save(insight);
    }

    /** Concatena os temas (título da tarefa + aula) das tarefas recentes. */
    private String buildRecentThemes(List<Task> recentTasks) {
        if (recentTasks.isEmpty()) {
            return "";
        }
        return recentTasks.stream()
                .map(t -> {
                    String lessonTitle = t.getLesson() != null ? t.getLesson().getTitle() : "aula";
                    String scorePart = t.getScore() != null ? ", nota " + t.getScore() + "/10" : "";
                    return t.getTitle() + " (" + lessonTitle + scorePart + ")";
                })
                .collect(Collectors.joining("; "));
    }

    /** Monta um resumo textual do desempenho a partir do progresso e das notas recentes. */
    private String buildPerformanceSummary(StudentProgress progress, List<Task> recentTasks) {
        String scoresPart = recentTasks.stream()
                .map(Task::getScore)
                .filter(java.util.Objects::nonNull)
                .map(String::valueOf)
                .collect(Collectors.joining(", "));
        String averagePart = recentTasks.stream()
                .map(Task::getScore)
                .filter(java.util.Objects::nonNull)
                .mapToInt(Integer::intValue)
                .average()
                .stream()
                .mapToObj(avg -> String.format(" Média das notas recentes: %.1f/10.", avg))
                .findFirst()
                .orElse("");

        String notesPart = scoresPart.isBlank() ? "" : " Notas recentes: " + scoresPart + "." + averagePart;

        if (progress == null) {
            return "Sem registro de progresso agregado. Tarefas concluídas recentemente: "
                    + recentTasks.size() + "." + notesPart;
        }
        return String.format(
                "Progresso geral: %.1f%%. Tarefas corrigidas: %d. Tarefas pendentes: %d. Concluídas recentemente: %d.%s",
                progress.getProgressPercentage(),
                progress.getCorrectedTasks(),
                progress.getPendingTasks(),
                recentTasks.size(),
                notesPart);
    }

    /** Monta o DTO de resposta derivando o módulo (da Lesson/progresso) e o status legível. */
    private StudentInsightResponse toResponse(StudentInsight insight, StudentProfile student) {
        String module = deriveModule(student);
        String status = toStatusLabel(student.getStatus());
        return StudentInsightResponse.from(insight, module, status);
    }

    /** Módulo derivado da aula da tarefa mais recente; fallback para o progresso. */
    private String deriveModule(StudentProfile student) {
        List<Task> recentTasks = taskRepository
                .findTop5ByStudentsIdAndStatusOrderByDueDateDesc(student.getId(), TaskStatus.CORRIGIDA);
        if (!recentTasks.isEmpty() && recentTasks.get(0).getLesson() != null) {
            return recentTasks.get(0).getLesson().getTitle();
        }
        return studentProgressRepository.findByStudentId(student.getId())
                .map(StudentProgress::getCurrentModule)
                .filter(m -> m != null && !m.isBlank())
                .orElse("N/A");
    }

    private String toStatusLabel(StudentStatus status) {
        return switch (status) {
            case EM_DIA -> "Em dia";
            case ATENCAO -> "Atenção";
            case EM_RISCO -> "Em risco";
        };
    }

    private StudentProfile findStudent(Long studentId) {
        return studentProfileRepository.findById(studentId)
                .orElseThrow(() -> new ResourceNotFoundException("Aluno não encontrado: " + studentId));
    }
}
