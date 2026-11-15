package com.edupulse.ingestion.services;

import java.util.List;
import java.util.UUID;

import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.ai.vectorstore.filter.Filter;
import org.springframework.ai.vectorstore.filter.FilterExpressionBuilder;
import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;

/**
 * Writes the child chunks to the vector store. Adding documents embeds them through the
 * configured Ollama embedding model; the chunk metadata (document, page, org unit, parent) is
 * stored next to each vector.
 */
@Service
@RequiredArgsConstructor
public class ChunkVectorStore {

  private final VectorStore vectorStore;

  /** Replaces all chunks of a document, so re-processing never leaves duplicates behind. */
  public void replace(UUID documentId, List<Document> children) {
    deleteByDocument(documentId);
    vectorStore.add(children);
  }

  public void deleteByDocument(UUID documentId) {
    Filter.Expression forDocument = new FilterExpressionBuilder()
        .eq(ChunkMetadata.DOCUMENT_ID, documentId.toString())
        .build();
    vectorStore.delete(forDocument);
  }
}
