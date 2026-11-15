package com.edupulse.ingestion.services;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.springframework.ai.document.Document;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.edupulse.ingestion.entities.DocumentStatus;
import com.edupulse.ingestion.entities.UploadedDocument;
import com.edupulse.ingestion.exceptions.DocumentNotFoundException;
import com.edupulse.ingestion.exceptions.UnsupportedDocumentException;
import com.edupulse.ingestion.repositories.UploadedDocumentRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class DocumentIngestionService {

  private static final int HEAD_BYTES = 8192;

  private final UploadedDocumentRepository repository;
  private final DocumentStorage storage;
  private final PageParser pageParser;
  private final ChunkingService chunkingService;
  private final ParentChunkStore parentChunkStore;
  private final ChunkVectorStore chunkVectorStore;

  public UploadedDocument upload(MultipartFile file, String orgUnit, String uploadedBy)
      throws IOException {
    if (file.isEmpty()) {
      throw new UnsupportedDocumentException("File is empty");
    }
    String filename = cleanFilename(file.getOriginalFilename());
    DocumentType type = DocumentType.detect(filename, readHead(file));

    UploadedDocument document = new UploadedDocument();
    document.setOrgUnit(orgUnit);
    document.setFilename(filename);
    document.setContentType(type.contentType());
    document.setSizeBytes(file.getSize());
    document.setUploadedBy(uploadedBy);
    document = repository.save(document);

    Path stored;
    try {
      stored = storage.save(document.getId(), file);
    } catch (IOException e) {
      fail(document, "Could not store the file");
      throw e;
    }

    log.info("Document {} ({}) uploaded by {} for {}", document.getId(), filename, uploadedBy, orgUnit);
    process(document, type, stored);
    return document;
  }

  public UploadedDocument get(UUID id, String orgUnit) {
    // other org units get the same 404 as a missing id, so existence is not
    // revealed
    return repository.findByIdAndOrgUnit(id, orgUnit).orElseThrow(DocumentNotFoundException::new);
  }

  private void process(UploadedDocument document, DocumentType type, Path stored) {
    document.setStatus(DocumentStatus.PROCESSING);
    repository.save(document);
    try {
      List<Document> pages = pageParser.parse(type, stored);
      long emptyPages = pages.stream().filter(p -> p.getText() == null || p.getText().isBlank()).count();
      log.info("Document {} parsed: {} pages, {} without text", document.getId(), pages.size(), emptyPages);

      ChunkingService.ChunkedDocument chunks = chunkingService.chunk(document, pages);
      if (chunks.children().isEmpty()) {
        fail(document, "No text could be extracted (scanned document?)");
        return;
      }
      parentChunkStore.replace(document.getId(), chunks.parents());
      log.info("Document {} chunked: {} parents, {} children",
          document.getId(), chunks.parents().size(), chunks.children().size());

      if (!embed(document, chunks.children())) {
        return;
      }
      log.info("Document {} embedded: {} chunks stored", document.getId(), chunks.children().size());

      document.setStatus(DocumentStatus.COMPLETED);
      document.setProcessedAt(Instant.now());
      repository.save(document);
    } catch (Exception e) {
      log.warn("Document {} failed to parse", document.getId(), e);
      fail(document, "Could not read the document");
    }
  }

  /** Embeds the children; on failure removes what was stored and marks the document FAILED. */
  private boolean embed(UploadedDocument document, List<Document> children) {
    try {
      chunkVectorStore.replace(document.getId(), children);
      return true;
    } catch (Exception e) {
      log.warn("Document {} could not be embedded", document.getId(), e);
      discardChunks(document);
      fail(document, "Could not embed the document");
      return false;
    }
  }

  /** Best-effort removal of everything a half-processed document left behind. */
  private void discardChunks(UploadedDocument document) {
    try {
      chunkVectorStore.deleteByDocument(document.getId());
    } catch (Exception e) {
      log.warn("Document {}: could not remove its vectors", document.getId(), e);
    }
    try {
      parentChunkStore.replace(document.getId(), List.of());
    } catch (Exception e) {
      log.warn("Document {}: could not remove its parents", document.getId(), e);
    }
  }

  private void fail(UploadedDocument document, String message) {
    document.setStatus(DocumentStatus.FAILED);
    document.setErrorMessage(message);
    document.setProcessedAt(Instant.now());
    repository.save(document);
  }

  private static byte[] readHead(MultipartFile file) throws IOException {
    try (InputStream in = file.getInputStream()) {
      return in.readNBytes(HEAD_BYTES);
    }
  }

  /** Display name only: never used to build a path. */
  private static String cleanFilename(String original) {
    if (original == null || original.isBlank()) {
      return "unnamed";
    }
    String name = original.substring(Math.max(original.lastIndexOf('/'), original.lastIndexOf('\\')) + 1);
    name = name.replaceAll("\\p{Cntrl}", "").trim();
    if (name.isEmpty()) {
      return "unnamed";
    }
    return name.length() > 255 ? name.substring(0, 255) : name;
  }
}
