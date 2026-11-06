package com.edupulse.gateway.configs;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.oauth2.server.resource.authentication.ReactiveJwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.ReactiveJwtGrantedAuthoritiesConverterAdapter;
import org.springframework.security.web.server.SecurityWebFilterChain;

import com.edupulse.common.docs.DocsPaths;
import com.edupulse.common.security.RoleClaims;

@Configuration
@EnableWebFluxSecurity
class SecurityConfig {

  @Bean
  SecurityWebFilterChain securityWebFilterChain(ServerHttpSecurity http,
      @Value("${springdoc.api-docs.enabled:false}") boolean docsEnabled) {
    return http
        .csrf(ServerHttpSecurity.CsrfSpec::disable)
        .authorizeExchange(exchanges -> {
          exchanges.pathMatchers("/api/auth/login", "/actuator/health").permitAll();
          if (docsEnabled) {
            exchanges.pathMatchers(DocsPaths.GATEWAY).permitAll();
          }
          exchanges.anyExchange().authenticated();
        })
        .oauth2ResourceServer(oauth2 -> oauth2
            .jwt(jwt -> jwt.jwtAuthenticationConverter(
                jwtAuthenticationConverter())))
        .build();
  }

  private ReactiveJwtAuthenticationConverter jwtAuthenticationConverter() {
    ReactiveJwtAuthenticationConverter converter = new ReactiveJwtAuthenticationConverter();
    converter.setJwtGrantedAuthoritiesConverter(
        new ReactiveJwtGrantedAuthoritiesConverterAdapter(RoleClaims.authoritiesConverter()));

    return converter;
  }

}
