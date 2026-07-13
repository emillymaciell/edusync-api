package com.edusync.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Configuração da documentação OpenAPI (Swagger UI).
 *
 * Define as informações gerais da API e registra um {@link SecurityScheme}
 * HTTP Bearer/JWT aplicado globalmente. Isso faz o botão "Authorize" (cadeado)
 * aparecer no Swagger UI, permitindo colar o token JWT uma única vez e testar
 * todas as rotas protegidas.
 *
 * Acesse a UI em: {@code http://localhost:8080/swagger-ui.html}
 */
@Configuration
public class OpenApiConfig {

    /** Nome do esquema de segurança referenciado pelo requisito global. */
    private static final String SECURITY_SCHEME_NAME = "bearerAuth";

    @Bean
    public OpenAPI eduSyncOpenAPI() {
        return new OpenAPI()
                .info(apiInfo())
                // Aplica o requisito de segurança globalmente (todas as rotas exibem o cadeado).
                .addSecurityItem(new SecurityRequirement().addList(SECURITY_SCHEME_NAME))
                .components(new Components().addSecuritySchemes(SECURITY_SCHEME_NAME, bearerJwtScheme()));
    }

    private Info apiInfo() {
        return new Info()
                .title("EduSync API")
                .description("""
                        API do EduSync — SaaS educacional (B2B2C) que centraliza a gestão para \
                        professores independentes e usa IA Generativa (Google Gemini) para criar \
                        materiais didáticos e exercícios.

                        Autenticação via JWT (RBAC): ADMIN, TEACHER e STUDENT.
                        Fluxo: faça login em /api/auth/login, copie o token e clique em "Authorize".""")
                .version("v1.0.0")
                .contact(new Contact().name("Equipe EduSync").email("suporte@edusync.com"))
                .license(new License().name("Proprietary"));
    }

    /**
     * Esquema HTTP Bearer com formato JWT. O {@code bearerFormat} é apenas
     * informativo/documental; o {@code scheme = "bearer"} é o que faz o Swagger
     * enviar o header {@code Authorization: Bearer <token>}.
     */
    private SecurityScheme bearerJwtScheme() {
        return new SecurityScheme()
                .name(SECURITY_SCHEME_NAME)
                .type(SecurityScheme.Type.HTTP)
                .scheme("bearer")
                .bearerFormat("JWT")
                .in(SecurityScheme.In.HEADER)
                .description("Insira o token JWT obtido em /api/auth/login (sem o prefixo 'Bearer ').");
    }
}
