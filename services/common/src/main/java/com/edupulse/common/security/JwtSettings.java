package com.edupulse.common.security;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;
import org.springframework.util.Assert;

/**
 * The JWT settings every service shares: who issued the tokens and where the signing keys can
 * be fetched. {@code jwt.issuer} is mandatory, so a service that forgets it fails at startup
 * instead of running without token validation.
 */
@ConfigurationProperties("jwt")
public record JwtSettings(
    String issuer,
    @DefaultValue("http://auth-service/.well-known/jwks.json") String jwksUri) {

  public JwtSettings {
    Assert.hasText(issuer, "jwt.issuer must be set");
  }
}
