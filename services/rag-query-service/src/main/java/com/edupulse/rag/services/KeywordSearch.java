package com.edupulse.rag.services;

import java.util.List;

import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;

/**
 * Full-text search over the child chunks using the generated content_tsv column. It finds what
 * embeddings are weak at: exact terms and regulatory codes ("27001", "Art. 5").
 */
@Service
@RequiredArgsConstructor
public class KeywordSearch {

  private final JdbcClient jdbc;

  public List<ChunkHit> search(String tsQuery, String orgUnit, int limit) {
    return jdbc.sql("""
        select id::text as id,
               content,
               metadata->>'documentId'   as document_id,
               metadata->>'documentName' as document_name,
               coalesce((metadata->>'page')::int, 1) as page,
               metadata->>'parentId'     as parent_id
          from ingestion.vector_store
         where content_tsv @@ to_tsquery('simple', :query)
           and metadata->>'orgUnit' = :orgUnit
         order by ts_rank_cd(content_tsv, to_tsquery('simple', :query)) desc
         limit :limit
        """)
        .param("query", tsQuery)
        .param("orgUnit", orgUnit)
        .param("limit", limit)
        .query((rs, rowNum) -> new ChunkHit(
            rs.getString("id"),
            rs.getString("content"),
            rs.getString("document_id"),
            rs.getString("document_name"),
            rs.getInt("page"),
            rs.getString("parent_id"),
            null))
        .list();
  }
}
