package br.com.fiap.petfamily.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI openAPI() {
        final String esquemaBearer = "bearerAuth";
        return new OpenAPI()
                // Habilita o botão "Authorize" do Swagger para testar rotas protegidas
                .components(new Components().addSecuritySchemes(esquemaBearer,
                        new SecurityScheme()
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")
                                .description("Token obtido em POST /auth/login (informe só o token, sem 'Bearer ')")))
                .addSecurityItem(new SecurityRequirement().addList(esquemaBearer))
                .info(new Info()
                        .title("Pet Family API")
                        .description(
                                "API RESTful para acompanhamento contínuo da saúde dos pets. " +
                                        "Desenvolvida como parte do Challenge CLYVO VET 2026 — FIAP.\n\n" +
                                        "**Funcionalidades:**\n" +
                                        "- Autenticação JWT com perfis TUTOR e VETERINARIO\n" +
                                        "- Cadastro de tutores e pets\n" +
                                        "- Agendamento, cancelamento e atendimento de consultas\n" +
                                        "- Cuidados preventivos com recorrência\n" +
                                        "- Assistente de IA simulada para saúde animal\n" +
                                        "- Dashboard com KPIs clínicos"
                        )
                        .version("1.0.0")
                        .contact(new Contact()
                                .name("Pedro Vaz & João Victor")
                                .email("pedrovazferreira10@gmail.com"))
                        .license(new License()
                                .name("FIAP — Challenge CLYVO VET 2026")));
    }
}