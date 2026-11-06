package com.edupulse.auth.dtos;

import com.edupulse.auth.entities.Role;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CreateUserRequest(
        @NotBlank @Size(min = 3, max = 100) String username,
        // Don't change from 72 (BCrypt)
        @NotBlank @Size(min = 8, max = 72) String password,
        @NotNull Role role,
        @NotBlank @Size(max = 100) String orgUnit) {

}
