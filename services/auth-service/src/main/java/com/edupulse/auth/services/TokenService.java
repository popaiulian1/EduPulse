package com.edupulse.auth.services;

import java.time.Instant;
import java.util.List;

import org.springframework.security.oauth2.jose.jws.SignatureAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Service;

import com.edupulse.auth.configs.JwtProperties;
import com.edupulse.auth.entities.AppUser;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class TokenService {
  private final JwtEncoder jwtEncoder;
  private final JwtProperties jwtProperties;

  public String generate(AppUser user) {
    Instant now = Instant.now();

    JwtClaimsSet claims = JwtClaimsSet.builder()
        .issuer("https://auth.api.edupulse")
        .subject(user.getUsername())
        .issuedAt(now)
        .expiresAt(now.plus(jwtProperties.expiresIn()))
        .claim("roles", List.of(user.getRole().name()))
        .claim("orgUnit", user.getOrgUnit())
        .build();

    JwsHeader header = JwsHeader.with(SignatureAlgorithm.RS256)
        .keyId("edupulse-auth-key-1")
        .build();

    return jwtEncoder.encode(JwtEncoderParameters.from(header, claims)).getTokenValue();
  }
}
