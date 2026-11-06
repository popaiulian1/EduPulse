package com.edupulse.ingestion.dto;

import java.time.Instant;
import java.util.UUID;

import com.edupulse.ingestion.entities.DocumentStatus;
import com.edupulse.ingestion.entities.UploadedDocument;

public record DocumentResponse(
    UUID id,
    String filename,
    String orgUnit,
    long sizeBytes,
    DocumentStatus status,
    String errorMessage,
    Instant uploadedAt,
    Instant processedAt) {

  public static DocumentResponse from(UploadedDocument d) {
    return new DocumentResponse(d.getId(), d.getFilename(), d.getOrgUnit(), d.getSizeBytes(),
        d.getStatus(), d.getErrorMessage(), d.getUploadedAt(), d.getProcessedAt());
  }
}
