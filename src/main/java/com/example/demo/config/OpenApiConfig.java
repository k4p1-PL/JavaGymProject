package com.example.demo.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** Opis REST API widoczny w Swagger UI: http://localhost:8080/swagger-ui.html */
@Configuration
public class OpenApiConfig {

    private static final String BEARER = "bearerAuth";

    @Bean
    public OpenAPI gymOpenApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("GymManager API")
                        .version("1.0")
                        .description("REST API systemu do obsługi siłowni: konta i role, karnety, płatności, "
                                + "grafik zajęć, rezerwacje, wejścia oraz import/eksport JSON i XML. "
                                + "Zaloguj się przez /api/auth/login i wklej token w przycisku Authorize."))
                .components(new Components().addSecuritySchemes(BEARER, new SecurityScheme()
                        .type(SecurityScheme.Type.HTTP)
                        .scheme("bearer")
                        .bearerFormat("JWT")))
                .addSecurityItem(new SecurityRequirement().addList(BEARER));
    }
}
