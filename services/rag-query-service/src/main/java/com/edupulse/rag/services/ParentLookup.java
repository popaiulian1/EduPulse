package com.edupulse.rag.services;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;

/** Loads the large parent chunks that give the LLM context around a matched child. */
@Service
@RequiredArgsConstructor
public class ParentLookup {

  private final JdbcClient jdbc;

  /** Parent text by parent id. The org-unit join is a second check on top of the filtered hits. */
  public Map<String, String> contents(Collection<String> parentIds, String orgUnit) {
    List<UUID> ids = parentIds.stream().filter(id -> id != null).map(UUID::fromString).toList();
    if (ids.isEmpty()) {
      return Map.of();
    }

    Map<String, String> contents = new HashMap<>();
    jdbc.sql("""
        select p.id::text as id, p.content
          from ingestion.document_parents p
          join ingestion.documents d on d.id = p.document_id
         where p.id in (:ids)
           and d.org_unit = :orgUnit
        """)
        .param("ids", ids)
        .param("orgUnit", orgUnit)
        .query((rs, rowNum) -> Map.entry(rs.getString("id"), rs.getString("content")))
        .list()
        .forEach(entry -> contents.put(entry.getKey(), entry.getValue()));
    return contents;
  }
}
