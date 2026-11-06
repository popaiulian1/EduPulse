package com.edupulse.ingestion.exceptions;

public class DocumentNotFoundException extends RuntimeException {
  public DocumentNotFoundException() {
    super("Document not found");
  }
}
