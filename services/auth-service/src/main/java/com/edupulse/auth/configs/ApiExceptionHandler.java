package com.edupulse.auth.configs;

import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import com.edupulse.auth.exceptions.UsernameAlreadyExistsException;

@RestControllerAdvice
public class ApiExceptionHandler {
  @ExceptionHandler(AuthenticationException.class)
  ProblemDetail handleAuthentication(AuthenticationException ex) {
    return ProblemDetail.forStatusAndDetail(HttpStatus.UNAUTHORIZED, "Invalid username or password");
  }

  @ExceptionHandler(UsernameAlreadyExistsException.class)
  ProblemDetail handleDuplicate(UsernameAlreadyExistsException ex) {
    return ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, "Username already exists");
  }

  @ExceptionHandler(HttpMessageNotReadableException.class)
  ProblemDetail handleBadBody(HttpMessageNotReadableException ex) {
    return ProblemDetail.forStatusAndDetail(HttpStatus.NOT_ACCEPTABLE, "Invalid request body");
  }

}
