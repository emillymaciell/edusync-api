package com.edusync.service;

import com.edusync.dto.response.GeneratedExerciseResponse;
import com.edusync.entity.Task;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

/**
 * Calcula a nota (0–10) de uma tarefa de múltipla escolha a partir do gabarito
 * em {@link Task#getDescription()} e das respostas em {@link Task#getStudentAnswer()}.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class QuizScoreCalculator {

    private final ObjectMapper objectMapper;

    /**
     * @return nota 0–10, ou {@code null} se a tarefa não for um quiz corrigível
     *         (descrição/resposta inválidas ou sem questões).
     */
    public Integer computeScore(Task task) {
        if (task == null || task.getDescription() == null || task.getDescription().isBlank()
                || task.getStudentAnswer() == null || task.getStudentAnswer().isBlank()) {
            return null;
        }

        try {
            GeneratedExerciseResponse quiz = objectMapper.readValue(
                    task.getDescription(), GeneratedExerciseResponse.class);
            List<GeneratedExerciseResponse.Questao> questoes = quiz.questoes();
            if (questoes == null || questoes.isEmpty()) {
                return null;
            }

            Map<String, String> answers = objectMapper.readValue(
                    task.getStudentAnswer(), new TypeReference<>() {});

            int correct = 0;
            for (int i = 0; i < questoes.size(); i++) {
                GeneratedExerciseResponse.Questao questao = questoes.get(i);
                String expected = questao.respostaCorreta();
                String selected = answers.get(String.valueOf(i));
                if (selected == null) {
                    selected = answers.get(Integer.toString(i));
                }
                if (expected != null && selected != null
                        && expected.trim().equalsIgnoreCase(selected.trim())) {
                    correct++;
                }
            }

            return (int) Math.round((correct * 10.0) / questoes.size());
        } catch (Exception e) {
            log.debug("[QuizScoreCalculator] Não foi possível calcular nota da tarefa {}: {}",
                    task.getId(), e.getMessage());
            return null;
        }
    }

    /**
     * Retorna a nota já persistida ou calcula (e opcionalmente o chamador persiste).
     */
    public Integer resolveScore(Task task) {
        if (task.getScore() != null) {
            return task.getScore();
        }
        return computeScore(task);
    }
}
