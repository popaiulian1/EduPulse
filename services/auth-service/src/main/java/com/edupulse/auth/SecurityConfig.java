package com.edupulse.auth;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Scaffold only: permits everything so /ping works.
 * TODO: permit /login and /register, require auth elsewhere, issue JWTs.
 */
@Configuration
class SecurityConfig {

	@Bean
	SecurityFilterChain securityFilterChain(HttpSecurity http) {
		return http
				.csrf(AbstractHttpConfigurer::disable)
				.authorizeHttpRequests(auth -> auth.anyRequest().permitAll())
				.build();
	}

}
