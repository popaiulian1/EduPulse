package com.edupulse.ingestion.services;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InOrder;

import com.edupulse.ingestion.entities.DocumentStatus;
import com.edupulse.ingestion.entities.UploadedDocument;
import com.edupulse.ingestion.exceptions.DocumentBusyException;
import com.edupulse.ingestion.exceptions.DocumentNotFoundException;
import com.edupulse.ingestion.repositories.UploadedDocumentRepository;

class DocumentDeletionTests {

  private static final String ORG = "org-1";

  private final UploadedDocumentRepository repository = mock(UploadedDocumentRepository.class);
  private final DocumentStorage storage = mock(DocumentStorage.class);
  private final ChunkVectorStore vectors = mock(ChunkVectorStore.class);

  private DocumentIngestionService service;
  private UploadedDocument document;

  @BeforeEach
  void setUp() {
    service = new DocumentIngestionService(repository, storage, mock(PageParser.class),
        mock(ChunkingService.class), mock(ParentChunkStore.class), vectors);
    document = new UploadedDocument();
    document.setId(UUID.randomUUID());
    document.setFilename("policy.pdf");
    document.setStatus(DocumentStatus.COMPLETED);
    when(repository.findByIdAndOrgUnit(document.getId(), ORG)).thenReturn(Optional.of(document));
  }

  @Test
  void removesVectorsThenFileThenDatabaseRow() throws IOException {
    service.delete(document.getId(), ORG, "admin");

    InOrder order = inOrder(vectors, storage, repository);
    order.verify(vectors).deleteByDocument(document.getId());
    order.verify(storage).delete(document.getId());
    order.verify(repository).delete(document);
  }

  @Test
  void keepsTheRowWhenVectorCleanupFailsSoTheDeleteCanBeRetried() throws IOException {
    doThrow(new RuntimeException("vector store down")).when(vectors).deleteByDocument(document.getId());

    assertThrows(RuntimeException.class, () -> service.delete(document.getId(), ORG, "admin"));

    verify(storage, never()).delete(any());
    verify(repository, never()).delete(any());
  }

  @Test
  void keepsTheRowWhenFileRemovalFails() throws IOException {
    doThrow(new IOException("disk")).when(storage).delete(document.getId());

    assertThrows(IOException.class, () -> service.delete(document.getId(), ORG, "admin"));

    verify(repository, never()).delete(any());
  }

  @Test
  void refusesWhileTheDocumentIsStillBeingIngested() {
    for (DocumentStatus busy : new DocumentStatus[] {DocumentStatus.PENDING, DocumentStatus.PROCESSING}) {
      document.setStatus(busy);
      assertThrows(DocumentBusyException.class, () -> service.delete(document.getId(), ORG, "admin"));
    }
    verifyNoInteractions(vectors, storage);
    verify(repository, never()).delete(any());
  }

  @Test
  void otherOrgUnitsGetNotFound() {
    assertThrows(DocumentNotFoundException.class,
        () -> service.delete(document.getId(), "other-org", "admin"));
    verifyNoInteractions(vectors, storage);
  }

  @Test
  void failedDocumentsCanBeDeleted() throws IOException {
    document.setStatus(DocumentStatus.FAILED);

    service.delete(document.getId(), ORG, "admin");

    verify(repository).delete(document);
  }
}
