package com.edupulse.ingestion.services;

import java.util.Locale;

import com.edupulse.ingestion.exceptions.UnsupportedDocumentException;

public enum DocumentType {
  PDF("application/pdf"),
  TEXT("text/plain");

  private final String contentType;

  DocumentType(String contentType) {
    this.contentType = contentType;
  }

  public String contentType() {
    return contentType;
  }

  /**
   * Decides by extension AND content, never by the client-supplied Content-Type.
   */
  public static DocumentType detect(String filename, byte[] head) {
    String name = filename.toLowerCase(Locale.ROOT);
    if (name.endsWith(".pdf")) {
      if (startsWith(head, "%PDF-")) {
        return PDF;
      }
      throw new UnsupportedDocumentException("File is not a valid PDF");
    }
    if (name.endsWith(".txt")) {
      for (byte b : head) {
        if (b == 0) {
          throw new UnsupportedDocumentException("File is not a text file");
        }
      }
      return TEXT;
    }
    throw new UnsupportedDocumentException("Unsupported file type; allowed: .pdf, .txt");
  }

  private static boolean startsWith(byte[] head, String prefix) {
    if (head.length < prefix.length()) {
      return false;
    }
    for (int i = 0; i < prefix.length(); i++) {
      if (head[i] != (byte) prefix.charAt(i)) {
        return false;
      }
    }
    return true;
  }
}
