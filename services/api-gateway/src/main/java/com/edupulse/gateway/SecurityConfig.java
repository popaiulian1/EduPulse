package com.edupulse.gateway;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.web.server.SecurityWebFilterChain;

/**
 * Scaffold only: permits everything so routes can be tested.
 * TODO: validate JWTs here (oauth2ResourceServer + jwk/public key from auth-service).
 */
@Configuration
@EnableWebFluxSecurity
class SecurityConfig {

	@Bean
	SecurityWebFilterChain securityWebFilterChain(ServerHttpSecurity http) {
		return http
				.csrf(ServerHttpSecurity.CsrfSpec::disable)
				.authorizeExchange(exchanges -> exchanges.anyExchange().permitAll())
				.build();
	}

}
