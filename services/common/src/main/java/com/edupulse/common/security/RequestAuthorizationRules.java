package com.edupulse.common.security;

import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AuthorizeHttpRequestsConfigurer;

/**
 * Extension point for the default filter chain in {@link JwtSecurityAutoConfiguration}.
 * A service declares beans of this type to add URL-level rules, for example
 * {@code registry.requestMatchers(HttpMethod.POST, "/documents").hasAuthority("ROLE_ADMIN")}.
 *
 * The rules are applied before the catch-all {@code anyRequest().authenticated()}, so they take
 * effect in the security filter, before Spring reads the request body (large uploads from the
 * wrong role are refused without being parsed or stored).
 */
@FunctionalInterface
public interface RequestAuthorizationRules {

  void customize(
      AuthorizeHttpRequestsConfigurer<HttpSecurity>.AuthorizationManagerRequestMatcherRegistry registry);
}
