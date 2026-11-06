package com.edupulse.ingestion.services;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

/**
 * Keeps the original uploads on disk. Files are named by the server-generated
 * id only.
 */
@Service
public class DocumentStorage {

  private final Path root;

  public DocumentStorage(@Value("${edupulse.storage.root:./uploads}") String root) {
    this.root = Path.of(root).toAbsolutePath().normalize();
  }

  public Path save(UUID id, MultipartFile file) throws IOException {
    Files.createDirectories(root);
    Path target = root.resolve(id.toString());
    file.transferTo(target);
    return target;
  }
}
