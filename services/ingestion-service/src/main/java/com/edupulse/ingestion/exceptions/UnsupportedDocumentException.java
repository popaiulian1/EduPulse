package com.edupulse.ingestion.exceptions;

public class UnsupportedDocumentException extends RuntimeException {
  public UnsupportedDocumentException(String message) {
    super(message);
  }
}
