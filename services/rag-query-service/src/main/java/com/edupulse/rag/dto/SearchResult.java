package com.edupulse.rag.dto;

import java.util.List;

/**
 * One retrieved passage with everything needed to cite it.
 *
 * @param snippet     the matched child chunk (what the citation quotes)
 * @param context     the parent chunk around it (what the LLM reads)
 * @param score       fused rank score from the semantic and keyword searches
 * @param similarity  cosine similarity of the snippet, or null if only the keyword search found it
 * @param rerankScore cross-encoder relevance from 0 to 1, or null when the result was not reranked
 * @param matchedBy   which searches found it: "semantic" and/or "keyword"
 */
public record SearchResult(
    String documentId,
    String documentName,
    int page,
    String snippet,
    String context,
    double score,
    Double similarity,
    Double rerankScore,
    List<String> matchedBy) {
}
