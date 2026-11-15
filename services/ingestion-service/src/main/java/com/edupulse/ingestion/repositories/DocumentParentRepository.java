package com.edupulse.ingestion.repositories;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.edupulse.ingestion.entities.DocumentParent;

public interface DocumentParentRepository extends JpaRepository<DocumentParent, UUID> {
  void deleteByDocumentId(UUID documentId);
}
