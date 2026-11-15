package com.edupulse.ingestion.services;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.springframework.ai.document.Document;
import org.springframework.ai.transformer.splitter.TokenTextSplitter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.edupulse.ingestion.entities.UploadedDocument;

/**
 * Parent/child chunking. Each page is split into large parent chunks (default 1000 tokens) that
 * give the LLM context, and each parent into small child chunks (default 200 tokens) that are
 * embedded for precise vector matching. A parent never spans pages, so every chunk has exactly
 * one page number to cite.
 *
 * No database or network access here: parents are stored by {@link ParentChunkStore} and the
 * children are embedded in a later step.
 */
@Service
public class ChunkingService {

  public record ParentChunk(UUID id, int pageNumber, int parentIndex, String content) {
  }

  public record ChunkedDocument(List<ParentChunk> parents, List<Document> children) {
  }

  private final TokenTextSplitter parentSplitter;
  private final TokenTextSplitter childSplitter;

  public ChunkingService(@Value("${edupulse.chunking.parent-tokens:1000}") int parentTokens,
      @Value("${edupulse.chunking.child-tokens:200}") int childTokens) {
    if (childTokens <= 0 || childTokens >= parentTokens) {
      throw new IllegalArgumentException(
          "edupulse.chunking: child-tokens must be positive and smaller than parent-tokens");
    }
    this.parentSplitter = TokenTextSplitter.builder().withChunkSize(parentTokens).build();
    this.childSplitter = TokenTextSplitter.builder().withChunkSize(childTokens).build();
  }

  public ChunkedDocument chunk(UploadedDocument document, List<Document> pages) {
    List<ParentChunk> parents = new ArrayList<>();
    List<Document> children = new ArrayList<>();
    int parentIndex = 0;

    for (Document rawPage : pages) {
      String text = TextCleaner.clean(rawPage.getText());
      if (text.isBlank()) {
        continue; // e.g. a scanned page without a text layer
      }
      int pageNumber = pageNumber(rawPage);
      Document page = Document.builder().text(text).metadata(rawPage.getMetadata()).build();

      for (Document parent : parentSplitter.split(page)) {
        if (isBlank(parent)) {
          continue;
        }
        UUID parentId = UUID.randomUUID();
        parents.add(new ParentChunk(parentId, pageNumber, parentIndex, parent.getText()));

        int childIndex = 0;
        for (Document child : childSplitter.split(parent)) {
          if (isBlank(child)) {
            continue;
          }
          children.add(Document.builder()
              .text(child.getText())
              .metadata(metadata(document, pageNumber, parentId, parentIndex, childIndex))
              .build());
          childIndex++;
        }
        parentIndex++;
      }
    }
    return new ChunkedDocument(parents, children);
  }

  private static Map<String, Object> metadata(UploadedDocument document, int pageNumber,
      UUID parentId, int parentIndex, int childIndex) {
    Map<String, Object> metadata = new LinkedHashMap<>();
    metadata.put(ChunkMetadata.DOCUMENT_ID, document.getId().toString());
    metadata.put(ChunkMetadata.DOCUMENT_NAME, document.getFilename());
    metadata.put(ChunkMetadata.PAGE, pageNumber);
    metadata.put(ChunkMetadata.ORG_UNIT, document.getOrgUnit());
    metadata.put(ChunkMetadata.PARENT_ID, parentId.toString());
    metadata.put(ChunkMetadata.PARENT_INDEX, parentIndex);
    metadata.put(ChunkMetadata.CHILD_INDEX, childIndex);
    return metadata;
  }

  private static int pageNumber(Document page) {
    Object value = page.getMetadata().get(PageParser.PAGE_NUMBER);
    return value instanceof Number number ? number.intValue() : 1;
  }

  private static boolean isBlank(Document document) {
    return document.getText() == null || document.getText().isBlank();
  }
}
