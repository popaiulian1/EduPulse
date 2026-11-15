package com.edupulse.ingestion.services;

/**
 * Metadata keys stored with every child chunk in the vector store. They are what citations are
 * built from (document name, page) and what the org-unit filter and the parent lookup use.
 */
public final class ChunkMetadata {

  public static final String DOCUMENT_ID = "documentId";
  public static final String DOCUMENT_NAME = "documentName";
  public static final String PAGE = "page";
  public static final String ORG_UNIT = "orgUnit";
  public static final String PARENT_ID = "parentId";
  public static final String PARENT_INDEX = "parentIndex";
  public static final String CHILD_INDEX = "childIndex";

  private ChunkMetadata() {
  }
}
