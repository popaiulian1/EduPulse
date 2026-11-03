package com.edupulse.auth.dto;

import java.time.Instant;
import java.util.UUID;

import com.edupulse.auth.entities.AppUser;
import com.edupulse.auth.entities.Role;

public record UserResponse(
    UUID id, String username, Role role, String orgUnit, boolean enabled, Instant createdAt) {

  public static UserResponse from(AppUser user) {
    return new UserResponse(user.getId(), user.getUsername(), user.getRole(),
        user.getOrgUnit(), user.getEnabled(), user.getCreatedAt());
  }
}
