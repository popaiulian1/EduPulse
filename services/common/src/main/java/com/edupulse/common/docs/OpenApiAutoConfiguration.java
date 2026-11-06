package com.edupulse.common.docs;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;

/**
 * Shared OpenAPI definition: the service name as the title and a "bearerAuth" scheme, so the
 * Authorize button in Swagger UI takes the JWT from /login. Only exists while the docs are
 * enabled.
 *
 * edupulse.docs.server-url is the path clients use to reach the service (for example /api/auth
 * behind the gateway), so "Try it out" calls the right URL.
 */
@AutoConfiguration
@ConditionalOnClass(OpenAPI.class)
@ConditionalOnProperty(name = "springdoc.api-docs.enabled", havingValue = "true")
public class OpenApiAutoConfiguration {

  @Bean
  @ConditionalOnMissingBean
  OpenAPI edupulseOpenApi(@Value("${spring.application.name}") String serviceName,
      @Value("${edupulse.docs.server-url:}") String serverUrl) {
    OpenAPI openApi = new OpenAPI()
        .info(new Info().title(serviceName).version("v1"))
        .components(new Components().addSecuritySchemes("bearerAuth",
            new SecurityScheme()
                .type(SecurityScheme.Type.HTTP)
                .scheme("bearer")
                .bearerFormat("JWT")))
        .addSecurityItem(new SecurityRequirement().addList("bearerAuth"));
    if (!serverUrl.isBlank()) {
      openApi.addServersItem(new Server().url(serverUrl));
    }
    return openApi;
  }
}
