package com.edupulse.common.security;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.cloud.client.loadbalancer.LoadBalanced;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.jose.jws.SignatureAlgorithm;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.client.RestTemplate;

import com.edupulse.common.docs.DocsPaths;

/**
 * Standard JWT resource-server setup for servlet services:
 * tokens issued by auth-service are validated with its
 * public key (found through service discovery), and everything
 * except the health endpoint requires authentication.
 *
 * A service opts in by adding
 * spring-boot-starter-security-oauth2-resource-server and setting
 * jwt.issuer. Each part backs off when the service defines its own
 */
@AutoConfiguration(after = JwtSettingsAutoConfiguration.class)
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
@ConditionalOnClass({ JwtDecoder.class, SecurityFilterChain.class, LoadBalanced.class })
@EnableMethodSecurity
public class JwtSecurityAutoConfiguration {

  @Bean
  @ConditionalOnMissingBean(JwtAuthenticationConverter.class)
  JwtAuthenticationConverter jwtAuthenticationConverter() {
    JwtAuthenticationConverter converter = new JwtAuthenticationConverter();
    converter.setJwtGrantedAuthoritiesConverter(RoleClaims.authoritiesConverter());
    return converter;
  }

  @SuppressWarnings("null")
  @Bean
  @ConditionalOnMissingBean(SecurityFilterChain.class)
  SecurityFilterChain jwtSecurityFilterChain(HttpSecurity http,
      JwtAuthenticationConverter converter,
      ObjectProvider<RequestAuthorizationRules> rules,
      @Value("${springdoc.api-docs.enabled:false}") boolean docsEnabled) {
    return http
        .csrf(AbstractHttpConfigurer::disable)
        .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
        .authorizeHttpRequests(auth -> {
          auth.requestMatchers("/actuator/health").permitAll();
          if (docsEnabled) {
            auth.requestMatchers(DocsPaths.SERVICE).permitAll();
          }
          // service-specific URL rules go before the catch-all
          rules.orderedStream().forEach(rule -> rule.customize(auth));
          auth.anyRequest().authenticated();
        })
        .oauth2ResourceServer(oauth2 -> oauth2
            .jwt(jwt -> jwt.jwtAuthenticationConverter(converter)))
        .build();
  }

  /** Only created when the service has no decoder of its own. */
  @Configuration(proxyBeanMethods = false)
  @ConditionalOnMissingBean(JwtDecoder.class)
  static class DiscoveryJwtDecoderConfiguration {

    @Bean
    @LoadBalanced
    RestTemplate jwksRestTemplate() {
      return new RestTemplate();
    }

    @Bean
    JwtDecoder jwtDecoder(@Qualifier("jwksRestTemplate") RestTemplate jwksRestTemplate,
        JwtSettings settings) {
      NimbusJwtDecoder decoder = NimbusJwtDecoder.withJwkSetUri(settings.jwksUri())
          .jwsAlgorithm(SignatureAlgorithm.RS256)
          .restOperations(jwksRestTemplate)
          .build();
      decoder.setJwtValidator(JwtValidators.createDefaultWithIssuer(settings.issuer()));
      return decoder;
    }
  }
}
