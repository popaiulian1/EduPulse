package com.edupulse.ingestion.services;

import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.edupulse.ingestion.entities.DocumentParent;
import com.edupulse.ingestion.repositories.DocumentParentRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ParentChunkStore {

  private final DocumentParentRepository repository;

  /** Replaces all parents of a document in one transaction, so re-processing never duplicates. */
  @Transactional
  public void replace(UUID documentId, List<ChunkingService.ParentChunk> parents) {
    repository.deleteByDocumentId(documentId);
    repository.saveAll(parents.stream().map(parent -> toEntity(documentId, parent)).toList());
  }

  private static DocumentParent toEntity(UUID documentId, ChunkingService.ParentChunk parent) {
    DocumentParent entity = new DocumentParent();
    entity.setId(parent.id());
    entity.setDocumentId(documentId);
    entity.setPageNumber(parent.pageNumber());
    entity.setParentIndex(parent.parentIndex());
    entity.setContent(parent.content());
    return entity;
  }
}
