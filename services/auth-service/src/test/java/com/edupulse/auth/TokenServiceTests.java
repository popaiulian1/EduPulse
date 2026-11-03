package com.edupulse.auth;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;

import com.edupulse.auth.configs.JwtProperties;
import com.edupulse.auth.entities.AppUser;
import com.edupulse.auth.repositories.UserRepository;
import com.edupulse.auth.services.TokenService;

@SpringBootTest
class TokenServiceTests {

  @Autowired
  TokenService tokenService;
  @Autowired
  UserRepository userRepository;
  @Autowired
  JwtProperties props;

  @Test
  void tokenRoundTrip() {
    AppUser admin = userRepository.findByUsername("admin").orElseThrow();
    String token = tokenService.generate(admin);

    Jwt jwt = NimbusJwtDecoder.withPublicKey(props.publicKey()).build().decode(token);

    assertEquals("admin", jwt.getSubject());
    assertEquals(List.of("ROLE_ADMIN"), jwt.getClaimAsStringList("roles"));
    assertEquals("SCHOOL_1", jwt.getClaimAsString("orgUnit"));
    assertEquals("http://localhost:8090", jwt.getIssuer().toString());
  }
}
