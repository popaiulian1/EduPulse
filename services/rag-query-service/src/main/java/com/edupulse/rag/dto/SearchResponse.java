package com.edupulse.rag.dto;

import java.util.List;

/**
 * @param expandedQueries the rewritten versions of the question that were searched as well;
 *                        empty when expansion is off or the LLM could not produce any
 * @param reranked        false when the reranker was disabled or unavailable, so the order is
 *                        the fused rank of the searches
 */
public record SearchResponse(
    String query,
    List<String> expandedQueries,
    boolean reranked,
    List<SearchResult> results) {
}
