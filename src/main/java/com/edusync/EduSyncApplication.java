package com.edusync;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Ponto de entrada da aplicação EduSync.
 * SaaS educacional (B2B2C) com CRM para professores e IA generativa.
 */
@SpringBootApplication
public class EduSyncApplication {

    public static void main(String[] args) {
        SpringApplication.run(EduSyncApplication.class, args);
    }
}
