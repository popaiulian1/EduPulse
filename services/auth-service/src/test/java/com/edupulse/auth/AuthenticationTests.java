package com.edupulse.auth;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;

@SpringBootTest
public class AuthenticationTests {

  @Autowired
  AuthenticationManager authenticationManager;

  @Test
  void adminCanAuthenticate() {
    Authentication auth = authenticationManager.authenticate(
        new UsernamePasswordAuthenticationToken("admin", "Admin123"));

    assertTrue(auth.isAuthenticated());
    assertEquals("ROLE_ADMIN", auth.getAuthorities().iterator().next().getAuthority());
  }

  @Test
  void wrongPasswordIsRejected() {
    assertThrows(BadCredentialsException.class, () -> authenticationManager.authenticate(
        new UsernamePasswordAuthenticationToken("admin", "wrong")));
  }
}
