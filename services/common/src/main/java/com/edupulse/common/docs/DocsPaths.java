package com.edupulse.common.docs;

/**
 * Paths that must be reachable without a token while the API docs are switched on
 * (springdoc.api-docs.enabled=true, which only the dev launch configurations do).
 */
public final class DocsPaths {

  /** Served by a service itself. */
  public static final String[] SERVICE = {
      "/v3/api-docs/**",
      "/swagger-ui.html",
      "/swagger-ui/**"
  };

  /** Served by the gateway: its own UI plus the docs of the services behind it. */
  public static final String[] GATEWAY = {
      "/v3/api-docs/**",
      "/swagger-ui.html",
      "/swagger-ui/**",
      "/webjars/**",
      "/api/*/v3/api-docs/**"
  };

  private DocsPaths() {
  }
}
