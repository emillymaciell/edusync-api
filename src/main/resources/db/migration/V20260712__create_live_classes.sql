-- Schema de Aulas ao Vivo (ddl-auto=update também cria via Hibernate).
CREATE TABLE IF NOT EXISTS live_classes (
    id                  BIGSERIAL PRIMARY KEY,
    title               VARCHAR(255)  NOT NULL,
    description         VARCHAR(2000) NOT NULL,
    scheduled_date_time TIMESTAMP     NOT NULL,
    completed           BOOLEAN       NOT NULL DEFAULT FALSE,
    teacher_profile_id  BIGINT        NOT NULL,
    CONSTRAINT fk_live_classes_teacher
        FOREIGN KEY (teacher_profile_id) REFERENCES teacher_profiles (id)
);

CREATE TABLE IF NOT EXISTS live_class_students (
    live_class_id       BIGINT NOT NULL,
    student_profile_id  BIGINT NOT NULL,
    PRIMARY KEY (live_class_id, student_profile_id),
    CONSTRAINT fk_live_class_students_class
        FOREIGN KEY (live_class_id) REFERENCES live_classes (id) ON DELETE CASCADE,
    CONSTRAINT fk_live_class_students_student
        FOREIGN KEY (student_profile_id) REFERENCES student_profiles (id) ON DELETE CASCADE
);

CREATE INDEX IF NOT EXISTS idx_live_classes_teacher_scheduled
    ON live_classes (teacher_profile_id, scheduled_date_time);
