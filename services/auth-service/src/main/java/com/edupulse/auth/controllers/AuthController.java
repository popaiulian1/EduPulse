package com.edupulse.auth.controllers;

import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.web.bind.annotation.RestController;

import com.edupulse.auth.configs.JwtProperties;
import com.edupulse.auth.dtos.LoginRequest;
import com.edupulse.auth.dtos.LoginResponse;
import com.edupulse.auth.entities.AppUser;
import com.edupulse.auth.repositories.UserRepository;
import com.edupulse.auth.services.TokenService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@Slf4j
@RestController
@RequiredArgsConstructor
public class AuthController {

  private final AuthenticationManager authenticationManager;
  private final UserRepository userRepository;
  private final TokenService tokenService;
  private final JwtProperties props;

  @PostMapping("/login")
  public LoginResponse login(@Valid @RequestBody LoginRequest request) {

    log.info("Login attempt for user {}", request.username());

    authenticationManager.authenticate(
        new UsernamePasswordAuthenticationToken(request.username(), request.password()));

    log.info("Login succeeded for user {}", request.username());

    AppUser user = userRepository.findByUsername(request.username()).orElseThrow();

    return new LoginResponse(
        tokenService.generate(user),
        "Bearer",
        props.expiresIn().toSeconds());
  }

}
