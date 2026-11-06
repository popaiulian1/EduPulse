package com.edupulse.auth.controllers;

import org.springframework.web.bind.annotation.RestController;

import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.RSAKey;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.Map;

import org.springframework.web.bind.annotation.GetMapping;

@Slf4j
@RestController
@RequiredArgsConstructor
public class JwksController {
  private final RSAKey rsaKey;

  @GetMapping("/.well-known/jwks.json")
  public Map<String, Object> jwks() {
    log.info("Requested well-known routes");
    return new JWKSet(rsaKey.toPublicJWK()).toJSONObject();
  }

}
