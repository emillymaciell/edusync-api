package com.edusync.domain.enums;

/**
 * Papéis de acesso do sistema (RBAC - Role-Based Access Control).
 *
 *   {@link #ADMIN}: aprova cadastros de professores, monitora métricas e gerencia Áreas de Ensino.
 *   {@link #TEACHER}: assinante do SaaS; cadastra alunos, agenda aulas e gera exercícios com IA.
 *   {@link #STUDENT}: consome aulas, responde exercícios e tem progresso rastreado.
 */
public enum Role {
    ADMIN,
    TEACHER,
    STUDENT
}
