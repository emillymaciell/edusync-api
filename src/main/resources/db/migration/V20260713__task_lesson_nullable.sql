-- Torna lesson_id opcional após Task.lesson nullable=true.
-- Hibernate ddl-auto=update NÃO remove a restrição NOT NULL existente.
ALTER TABLE tasks ALTER COLUMN lesson_id DROP NOT NULL;
