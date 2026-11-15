package com.edupulse.rag.services;

import java.util.List;
import java.util.Map;

import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.ai.vectorstore.filter.Filter;
import org.springframework.ai.vectorstore.filter.FilterExpressionBuilder;
import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;

/**
 * Vector similarity search over the child chunks. The org-unit filter is part of the vector
 * query itself, so other units' chunks never enter the candidate list.
 */
@Service
@RequiredArgsConstructor
public class SemanticSearch {

  private final VectorStore vectorStore;

  public List<ChunkHit> search(String query, String orgUnit, int limit) {
    Filter.Expression sameOrgUnit = new FilterExpressionBuilder().eq("orgUnit", orgUnit).build();

    SearchRequest request = SearchRequest.builder()
        .query(query)
        .topK(limit)
        .filterExpression(sameOrgUnit)
        .build();

    return vectorStore.similaritySearch(request).stream().map(SemanticSearch::toHit).toList();
  }

  private static ChunkHit toHit(Document document) {
    Map<String, Object> metadata = document.getMetadata();
    return new ChunkHit(
        document.getId(),
        document.getText(),
        text(metadata, "documentId"),
        text(metadata, "documentName"),
        metadata.get("page") instanceof Number page ? page.intValue() : 1,
        text(metadata, "parentId"),
        document.getScore());
  }

  private static String text(Map<String, Object> metadata, String key) {
    Object value = metadata.get(key);
    return value == null ? null : value.toString();
  }
}
