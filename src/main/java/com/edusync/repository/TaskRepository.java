package com.edusync.repository;

import com.edusync.domain.enums.TaskStatus;
import com.edusync.entity.Task;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface TaskRepository extends JpaRepository<Task, Long> {

    List<Task> findByLessonId(Long lessonId);

    List<Task> findByStudentsId(Long studentProfileId);

    /** Tarefas atribuídas ao aluno identificado pelo ID do usuário (User.id). */
    @Query("""
            SELECT DISTINCT t FROM Task t
            JOIN t.students s
            JOIN s.user u
            WHERE u.id = :userId
            ORDER BY t.dueDate DESC
            """)
    List<Task> findByStudentProfileUserId(@Param("userId") Long userId);

    /** Tarefa atribuída ao aluno identificado pelo ID do usuário (User.id). */
    @Query("""
            SELECT t FROM Task t
            JOIN t.students s
            JOIN s.user u
            WHERE t.id = :taskId AND u.id = :userId
            """)
    Optional<Task> findByIdAndStudentProfileUserId(@Param("taskId") Long taskId, @Param("userId") Long userId);

    /** Tarefa do professor com alunos carregados (validação de ownership na correção). */
    @Query("""
            SELECT t FROM Task t
            JOIN FETCH t.students s
            JOIN FETCH s.user
            JOIN t.lesson l
            WHERE t.id = :taskId AND l.teacher.id = :teacherId
            """)
    Optional<Task> findByIdAndLesson_Teacher_Id(@Param("taskId") Long taskId, @Param("teacherId") Long teacherId);

    /**
     * Tarefas do professor com alunos e usuários carregados (evita LazyInitialization
     * com spring.jpa.open-in-view=false).
     */
    @Query("""
            SELECT DISTINCT t FROM Task t
            JOIN FETCH t.students s
            JOIN FETCH s.user
            JOIN t.lesson l
            WHERE l.teacher.id = :teacherId
            ORDER BY t.dueDate DESC
            """)
    List<Task> findByTeacherWithStudents(@Param("teacherId") Long teacherProfileId);

    /** Tarefas das aulas de um professor, ordenadas pelo prazo mais recente. */
    List<Task> findByLesson_Teacher_IdOrderByDueDateDesc(Long teacherProfileId);

    /**
     * Últimas tarefas de um aluno em um dado status (ex.: CORRIGIDA = concluídas),
     * ordenadas pelo prazo mais recente. Usado para montar o insight de IA.
     */
    List<Task> findTop5ByStudentsIdAndStatusOrderByDueDateDesc(Long studentProfileId, TaskStatus status);

    /** Conta tarefas de um professor (via aula) em um dado status. */
    long countByLesson_Teacher_IdAndStatus(Long teacherProfileId, TaskStatus status);
}
