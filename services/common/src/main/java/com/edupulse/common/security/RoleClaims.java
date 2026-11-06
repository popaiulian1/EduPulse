package com.edupulse.common.security;

import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;

/**
 * token's {@code roles} claim are already stored with
 * their {@code ROLE_} prefix ("ROLE_ADMIN"), so no extra prefix is added.
 */
public final class RoleClaims {

  public static final String CLAIM = "roles";

  private RoleClaims() {
  }

  public static JwtGrantedAuthoritiesConverter authoritiesConverter() {
    JwtGrantedAuthoritiesConverter converter = new JwtGrantedAuthoritiesConverter();
    converter.setAuthoritiesClaimName(CLAIM);
    converter.setAuthorityPrefix("");
    return converter;
  }
}
