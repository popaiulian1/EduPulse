package com.edupulse.ingestion.services;

import java.nio.file.Path;
import java.util.List;

import org.springframework.ai.document.Document;
import org.springframework.ai.reader.TextReader;
import org.springframework.ai.reader.pdf.PagePdfDocumentReader;
import org.springframework.core.io.FileSystemResource;
import org.springframework.stereotype.Service;

/**
 * Turns a stored file into one Document per page, each with a page_number in
 * its metadata.
 */
@Service
public class PageParser {

  public static final String PAGE_NUMBER = PagePdfDocumentReader.METADATA_START_PAGE_NUMBER;

  public List<Document> parse(DocumentType type, Path file) {
    FileSystemResource resource = new FileSystemResource(file);
    return switch (type) {
      case PDF -> new PagePdfDocumentReader(resource).get();
      case TEXT -> {
        List<Document> documents = new TextReader(resource).get();
        documents.forEach(d -> d.getMetadata().put(PAGE_NUMBER, 1));
        yield documents;
      }
    };
  }
}
