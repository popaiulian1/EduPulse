package com.edupulse.ingestion.services;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Test;
import org.springframework.ai.document.Document;

import com.edupulse.ingestion.entities.UploadedDocument;
import com.knuddels.jtokkit.Encodings;
import com.knuddels.jtokkit.api.Encoding;
import com.knuddels.jtokkit.api.EncodingType;

class ChunkingServiceTests {

  private static final String SENTENCE =
      "Section 4.2 requires that all customer records are retained for seven years. ";

  private final Encoding encoding =
      Encodings.newDefaultEncodingRegistry().getEncoding(EncodingType.CL100K_BASE);

  private final ChunkingService service = new ChunkingService(1000, 200);

  @Test
  void splitsPagesIntoParentsAndChildrenThatPointBackToTheirParent() {
    UploadedDocument document = document();
    List<Document> pages = List.of(page(SENTENCE.repeat(250), 1), page(SENTENCE.repeat(250), 2));

    ChunkingService.ChunkedDocument result = service.chunk(document, pages);

    assertTrue(result.parents().size() >= 4, "two long pages should give several parents");
    assertTrue(result.children().size() > result.parents().size());

    Set<String> parentIds = result.parents().stream()
        .map(p -> p.id().toString()).collect(Collectors.toSet());
    for (Document child : result.children()) {
      assertTrue(parentIds.contains((String) child.getMetadata().get(ChunkMetadata.PARENT_ID)),
          "every child must reference an existing parent");
    }
  }

  @Test
  void respectsTheTokenSizes() {
    ChunkingService.ChunkedDocument result =
        service.chunk(document(), List.of(page(SENTENCE.repeat(400), 1)));

    for (ChunkingService.ParentChunk parent : result.parents()) {
      assertTrue(encoding.countTokens(parent.content()) <= 1000, "parent over 1000 tokens");
    }
    for (Document child : result.children()) {
      assertTrue(encoding.countTokens(child.getText()) <= 200, "child over 200 tokens");
    }
  }

  @Test
  void keepsTheCitationMetadataOnEveryChild() {
    UploadedDocument document = document();
    ChunkingService.ChunkedDocument result =
        service.chunk(document, List.of(page(SENTENCE.repeat(100), 7)));

    assertFalse(result.children().isEmpty());
    for (Document child : result.children()) {
      Map<String, Object> metadata = child.getMetadata();
      assertEquals(document.getId().toString(), metadata.get(ChunkMetadata.DOCUMENT_ID));
      assertEquals("policy.pdf", metadata.get(ChunkMetadata.DOCUMENT_NAME));
      assertEquals("SCHOOL_1", metadata.get(ChunkMetadata.ORG_UNIT));
      assertEquals(7, metadata.get(ChunkMetadata.PAGE));
    }
  }

  @Test
  void aParentNeverSpansPages() {
    ChunkingService.ChunkedDocument result = service.chunk(document(),
        List.of(page(SENTENCE.repeat(30), 1), page(SENTENCE.repeat(30), 2)));

    Set<Integer> pages = result.parents().stream()
        .map(ChunkingService.ParentChunk::pageNumber).collect(Collectors.toSet());
    assertEquals(Set.of(1, 2), pages);
    for (ChunkingService.ParentChunk parent : result.parents()) {
      assertFalse(parent.content().isBlank());
    }
  }

  @Test
  void skipsPagesWithoutText() {
    ChunkingService.ChunkedDocument result = service.chunk(document(),
        List.of(page("   ", 1), page(SENTENCE.repeat(10), 2)));

    assertEquals(Set.of(2), result.parents().stream()
        .map(ChunkingService.ParentChunk::pageNumber).collect(Collectors.toSet()));
  }

  @Test
  void aDocumentWithoutTextProducesNothing() {
    ChunkingService.ChunkedDocument result =
        service.chunk(document(), List.of(page("", 1), page("  \n ", 2)));

    assertTrue(result.parents().isEmpty());
    assertTrue(result.children().isEmpty());
  }

  @Test
  void cleansWhitespaceNoiseBeforeSplitting() {
    String noisy = "Section      4.2   requires\t\tretention   of   records.\n\n\n\n   ".repeat(60);

    ChunkingService.ChunkedDocument result = service.chunk(document(), List.of(page(noisy, 1)));

    assertFalse(result.parents().isEmpty());
    for (ChunkingService.ParentChunk parent : result.parents()) {
      assertFalse(parent.content().contains("  "), "no runs of spaces");
      assertFalse(parent.content().contains("\t"), "no tabs");
      assertFalse(parent.content().contains("\n\n\n"), "at most one blank line");
    }
    for (Document child : result.children()) {
      assertFalse(child.getText().contains("  "));
    }
  }

  @Test
  void indexesAreSequential() {
    ChunkingService.ChunkedDocument result =
        service.chunk(document(), List.of(page(SENTENCE.repeat(250), 1)));

    for (int i = 0; i < result.parents().size(); i++) {
      assertEquals(i, result.parents().get(i).parentIndex());
    }
  }

  @Test
  void rejectsASizeConfigurationWhereChildrenAreNotSmallerThanParents() {
    assertThrows(IllegalArgumentException.class, () -> new ChunkingService(200, 200));
    assertThrows(IllegalArgumentException.class, () -> new ChunkingService(1000, 0));
  }

  private static UploadedDocument document() {
    UploadedDocument document = new UploadedDocument();
    document.setId(UUID.randomUUID());
    document.setFilename("policy.pdf");
    document.setOrgUnit("SCHOOL_1");
    return document;
  }

  private static Document page(String text, int pageNumber) {
    return new Document(text, Map.of(PageParser.PAGE_NUMBER, pageNumber));
  }
}
