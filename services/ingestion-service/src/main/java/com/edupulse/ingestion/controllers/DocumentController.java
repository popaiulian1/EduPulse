package com.edupulse.ingestion.controllers;

import java.io.IOException;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.edupulse.ingestion.dto.DocumentResponse;
import com.edupulse.ingestion.services.DocumentIngestionService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/documents")
@RequiredArgsConstructor
class DocumentController {

  private final DocumentIngestionService ingestionService;

  @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
  @PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'ROLE_COMPLIANCE_OFFICER')")
  ResponseEntity<DocumentResponse> upload(@RequestParam("file") MultipartFile file,
      @AuthenticationPrincipal Jwt jwt) throws IOException {
    var document = ingestionService.upload(file, orgUnit(jwt), jwt.getSubject());
    return ResponseEntity.status(HttpStatus.ACCEPTED).body(DocumentResponse.from(document));
  }

  @GetMapping("/{id}")
  DocumentResponse get(@PathVariable UUID id, @AuthenticationPrincipal Jwt jwt) {
    return DocumentResponse.from(ingestionService.get(id, orgUnit(jwt)));
  }

  private static String orgUnit(Jwt jwt) {
    String orgUnit = jwt.getClaimAsString("orgUnit");
    if (orgUnit == null || orgUnit.isBlank()) {
      throw new AccessDeniedException("Token has no orgUnit");
    }
    return orgUnit;
  }
}
