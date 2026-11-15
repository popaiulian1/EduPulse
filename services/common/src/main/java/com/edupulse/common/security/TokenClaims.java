package com.edupulse.common.security;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.oauth2.jwt.Jwt;

/** Reads the custom claims auth-service puts into every token. */
public final class TokenClaims {

  public static final String ORG_UNIT = "orgUnit";

  private TokenClaims() {
  }

  /**
   * The caller's organization unit. It always comes from the verified token and never from the
   * request, so a caller cannot read or write another unit's data by asking for it.
   */
  public static String orgUnit(Jwt jwt) {
    String orgUnit = jwt.getClaimAsString(ORG_UNIT);
    if (orgUnit == null || orgUnit.isBlank()) {
      throw new AccessDeniedException("Token has no orgUnit");
    }
    return orgUnit;
  }
}
