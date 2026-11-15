package com.edupulse.rag.services;

/** A child chunk found by one of the searches, with its citation metadata. */
public record ChunkHit(
    String id,
    String content,
    String documentId,
    String documentName,
    int page,
    String parentId,
    Double similarity) {
}
