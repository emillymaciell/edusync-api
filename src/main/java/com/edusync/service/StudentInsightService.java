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
    private final QuizScoreCalculator quizScoreCalculator;

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

    /**
     * Força a regeneração da análise via IA, sobrescrevendo o registro existente
     * do aluno (upsert). Ignora o cache de 24h.
     */
    @Transactional
    public StudentInsightResponse generateAnalysis(Long studentId) {
        StudentProfile student = findStudent(studentId);

        StudentInsight existing = studentInsightRepository
                .findFirstByStudentProfileIdOrderByUpdatedAtDesc(studentId)
                .orElse(null);

        log.info("[Insight] Refresh forçado da análise do aluno {} (existente={})",
                studentId, existing != null);
        StudentInsight updated = generateAndPersist(student, existing);
        return toResponse(updated, student);
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
        Long studentId = student.getId();

        List<Task> correctedTasks = taskRepository
                .findByStudentsIdAndStatus(studentId, TaskStatus.CORRIGIDA);
        ensureScores(correctedTasks);

        List<Task> recentCorrectedTasks = correctedTasks.stream()
                .sorted((a, b) -> {
                    if (a.getDueDate() == null && b.getDueDate() == null) return 0;
                    if (a.getDueDate() == null) return 1;
                    if (b.getDueDate() == null) return -1;
                    return b.getDueDate().compareTo(a.getDueDate());
                })
                .limit(10)
                .toList();

        String studentName = student.getUser().getName();
        String recentThemes = buildRecentThemes(recentCorrectedTasks);
        String performanceSummary = buildPerformanceSummary(studentId, recentCorrectedTasks);

        GeneratedInsightResponse ai = geminiIntegrationService
                .generateInsight(studentName, recentThemes, performanceSummary);

        StudentInsight insight = existing != null ? existing : new StudentInsight();
        insight.setStudentProfile(student);
        insight.setAnalysis(ai.analysis());
        if (insight.getRecommendations() == null) {
            insight.setRecommendations(new ArrayList<>());
        } else {
            insight.getRecommendations().clear();
        }
        if (ai.recommendations() != null) {
            insight.getRecommendations().addAll(ai.recommendations());
        }
        insight.setUpdatedAt(LocalDateTime.now());

        return studentInsightRepository.save(insight);
    }

    /**
     * Preenche {@code score} em tarefas corrigidas que ainda não têm nota,
     * comparando gabarito e resposta do aluno, e persiste o resultado.
     */
    private void ensureScores(List<Task> tasks) {
        for (Task task : tasks) {
            if (task.getScore() != null) {
                continue;
            }
            Integer computed = quizScoreCalculator.computeScore(task);
            if (computed != null) {
                task.setScore(computed);
                taskRepository.save(task);
                log.info("[Insight] Nota retroativa calculada para tarefa {}: {}/10",
                        task.getId(), computed);
            }
        }
    }

    /** Concatena os temas (título da tarefa + aula) das tarefas recentes corrigidas. */
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

    /**
     * Monta o resumo de desempenho com contagens reais e notas por tarefa.
     */
    private String buildPerformanceSummary(Long studentId, List<Task> recentCorrectedTasks) {
        long totalTasks = taskRepository.countByStudentsId(studentId);
        long correctedTasks = taskRepository.countByStudentsIdAndStatus(studentId, TaskStatus.CORRIGIDA);
        long pendingTasks = taskRepository.countByStudentsIdAndStatus(studentId, TaskStatus.PENDENTE);
        long deliveredTasks = taskRepository.countByStudentsIdAndStatus(studentId, TaskStatus.ENTREGUE);
        long overdueTasks = taskRepository.countByStudentsIdAndStatus(studentId, TaskStatus.ATRASADA);

        StringBuilder notesDetail = new StringBuilder();
        List<Integer> scores = new ArrayList<>();
        for (Task task : recentCorrectedTasks) {
            String title = task.getTitle() != null ? task.getTitle() : "Sem título";
            if (task.getScore() != null) {
                scores.add(task.getScore());
                notesDetail.append("- ").append(title).append(": ").append(task.getScore()).append("/10. ");
            } else {
                notesDetail.append("- ").append(title).append(": sem nota registrada. ");
            }
        }

        String averagePart = "";
        if (!scores.isEmpty()) {
            double avg = scores.stream().mapToInt(Integer::intValue).average().orElse(0);
            averagePart = String.format(" Média das notas: %.1f/10.", avg);
        }

        String notesPart = recentCorrectedTasks.isEmpty()
                ? " Nenhuma tarefa corrigida com nota disponível."
                : " Notas por tarefa (0 a 10): " + notesDetail + averagePart;

        return String.format(
                "Total de tarefas: %d. Tarefas já corrigidas: %d. Tarefas pendentes: %d. "
                        + "Tarefas entregues aguardando correção: %d. Tarefas atrasadas: %d.%s",
                totalTasks,
                correctedTasks,
                pendingTasks,
                deliveredTasks,
                overdueTasks,
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
