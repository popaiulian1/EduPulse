package com.edupulse.auth.controllers;

import org.springframework.web.bind.annotation.RestController;

import com.edupulse.auth.dtos.CreateUserRequest;
import com.edupulse.auth.dtos.UserResponse;
import com.edupulse.auth.entities.AppUser;
import com.edupulse.auth.services.UserService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@RestController
@RequiredArgsConstructor
public class UserController {
  private final UserService userService;

  @PostMapping("/users")
  @PreAuthorize("hasRole('ADMIN')")
  public ResponseEntity<UserResponse> create(@Valid @RequestBody CreateUserRequest request) {
    AppUser created = userService.create(request);
    return ResponseEntity.status(HttpStatus.CREATED).body(UserResponse.from(created));
  }

}
