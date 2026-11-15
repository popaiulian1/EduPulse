package com.edupulse.ingestion.exceptions;

import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
class ApiExceptionHandler {

  @ExceptionHandler(UnsupportedDocumentException.class)
  ProblemDetail handleUnsupported(UnsupportedDocumentException ex) {
    return ProblemDetail.forStatusAndDetail(HttpStatus.UNSUPPORTED_MEDIA_TYPE, ex.getMessage());
  }

  @ExceptionHandler(DocumentNotFoundException.class)
  ProblemDetail handleNotFound(DocumentNotFoundException ex) {
    return ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, ex.getMessage());
  }

  @ExceptionHandler(DocumentBusyException.class)
  ProblemDetail handleBusy(DocumentBusyException ex) {
    return ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, ex.getMessage());
  }
}
