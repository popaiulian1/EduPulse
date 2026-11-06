package com.edupulse.ingestion.entities;

import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "documents")
public class UploadedDocument {

  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  private UUID id;

  private String orgUnit;
  private String filename;
  private String contentType;
  private long sizeBytes;

  @Enumerated(EnumType.STRING)
  private DocumentStatus status = DocumentStatus.PENDING;

  private String errorMessage;
  private String uploadedBy;
  private Instant uploadedAt = Instant.now();
  private Instant processedAt;
}
