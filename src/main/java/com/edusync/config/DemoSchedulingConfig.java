package com.edusync.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Habilita o agendamento usado apenas para limpar IPs expirados do limitador da demo.
 * O projeto não tinha {@code @EnableScheduling}. Nenhum job antigo passa a rodar.
 */
@Configuration
@EnableScheduling
public class DemoSchedulingConfig {
}
