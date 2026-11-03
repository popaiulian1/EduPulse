package com.edupulse.auth.services;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import com.edupulse.auth.dto.CreateUserRequest;
import com.edupulse.auth.entities.AppUser;
import com.edupulse.auth.exceptions.UsernameAlreadyExistsException;
import com.edupulse.auth.repositories.UserRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class UserService {

  private final UserRepository userRepository;
  private final PasswordEncoder passwordEncoder;

  public AppUser create(CreateUserRequest request) {
    String username = request.username().trim();

    if (userRepository.existsByUsername(username)) {
      throw new UsernameAlreadyExistsException();
    }

    AppUser user = new AppUser();
    user.setUsername(username);
    user.setPasswordHash(passwordEncoder.encode(request.password()));
    user.setRole(request.role());
    user.setOrgUnit(request.orgUnit().trim());
    return userRepository.save(user);
  }
}
