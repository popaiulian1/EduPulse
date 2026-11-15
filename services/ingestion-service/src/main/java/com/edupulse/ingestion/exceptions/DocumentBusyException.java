package com.edupulse.ingestion.exceptions;

/** The document is still being ingested, so it cannot be removed yet. */
public class DocumentBusyException extends RuntimeException {
  public DocumentBusyException() {
    super("Document is still being processed and cannot be deleted yet");
  }
}
