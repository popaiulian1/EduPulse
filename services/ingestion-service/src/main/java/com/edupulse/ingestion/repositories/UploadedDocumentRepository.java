package com.edupulse.ingestion.repositories;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.edupulse.ingestion.entities.UploadedDocument;

public interface UploadedDocumentRepository extends JpaRepository<UploadedDocument, UUID> {
  Optional<UploadedDocument> findByIdAndOrgUnit(UUID id, String orgUnit);
}
