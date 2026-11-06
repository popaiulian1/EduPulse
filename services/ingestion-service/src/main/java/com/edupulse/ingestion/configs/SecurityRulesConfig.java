package com.edupulse.ingestion.configs;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;

import com.edupulse.common.security.RequestAuthorizationRules;

/**
 * URL-level rules for ingestion-service, applied by the shared filter chain from common.
 * Uploads are limited to admins and compliance officers here, in the security filter, so a user
 * without the role is refused before the (up to 200 MB) body is read. The @PreAuthorize on the
 * controller stays as a second line of defence.
 */
@Configuration
class SecurityRulesConfig {

  @Bean
  RequestAuthorizationRules documentUploadRules() {
    return registry -> registry
        .requestMatchers(HttpMethod.POST, "/documents")
        .hasAnyAuthority("ROLE_ADMIN", "ROLE_COMPLIANCE_OFFICER");
  }
}
