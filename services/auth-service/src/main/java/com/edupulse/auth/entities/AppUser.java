package com.edupulse.auth.entities;

import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

@Data
@Entity
@Table(name = "users")
public class AppUser {
  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  UUID id;

  @NotNull
  String username;

  @NotNull
  String passwordHash;

  @NotNull
  @Enumerated(EnumType.STRING)
  Role role;

  @NotNull
  @NotBlank
  @Pattern(regexp = "^[A-Z0-9_]{1,100}$", message = "orgUnit must be uppercase letters, digits and underscores")
  String orgUnit;

  @NotNull
  Boolean enabled = true;

  @NotNull
  Instant createdAt = Instant.now();
}
