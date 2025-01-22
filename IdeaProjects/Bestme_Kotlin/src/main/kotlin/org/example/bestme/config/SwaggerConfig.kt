package org.example.bestme.config

import io.swagger.v3.oas.models.OpenAPI
import io.swagger.v3.oas.models.Components
import io.swagger.v3.oas.models.security.SecurityRequirement
import io.swagger.v3.oas.models.security.SecurityScheme
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class SwaggerConfig {

    @Bean
    fun api(): OpenAPI {
        val access = SecurityScheme()
            .type(SecurityScheme.Type.APIKEY)
            .`in`(SecurityScheme.In.HEADER)
            .name("Authorization")
            .bearerFormat("JWT")

        val refresh = SecurityScheme()
            .type(SecurityScheme.Type.APIKEY)
            .`in`(SecurityScheme.In.HEADER)
            .name("refresh")
            .bearerFormat("JWT")

        val securityRequirement = SecurityRequirement()
            .addList("Bearer Token")
            .addList("Refresh Token")

        return OpenAPI()
            .components(
                Components()
                    .addSecuritySchemes("Bearer Token", access)
                    .addSecuritySchemes("Refresh Token", refresh)
            )
            .addSecurityItem(securityRequirement)
    }
}
