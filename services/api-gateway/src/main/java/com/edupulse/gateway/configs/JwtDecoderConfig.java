package com.edupulse.gateway.configs;

import org.springframework.cloud.client.loadbalancer.LoadBalanced;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.oauth2.jose.jws.SignatureAlgorithm;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusReactiveJwtDecoder;
import org.springframework.security.oauth2.jwt.ReactiveJwtDecoder;
import org.springframework.web.reactive.function.client.WebClient;

import lombok.RequiredArgsConstructor;

@Configuration
@RequiredArgsConstructor
public class JwtDecoderConfig {
  private final JwtProperties jwtProperties;

  @Bean
  @LoadBalanced
  WebClient.Builder loadBalancedWebClientBuilder() {
    return WebClient.builder();
  }

  @Bean
  ReactiveJwtDecoder jwtDecoder(@LoadBalanced WebClient.Builder builder) {

    NimbusReactiveJwtDecoder decoder = NimbusReactiveJwtDecoder
        .withJwkSetUri("http://auth-service/.well-known/jwks.json")
        .jwsAlgorithm(SignatureAlgorithm.RS256)
        .webClient(builder.build())
        .build();

    decoder.setJwtValidator(JwtValidators.createDefaultWithIssuer(jwtProperties.issuer()));

    return decoder;
  }
}
