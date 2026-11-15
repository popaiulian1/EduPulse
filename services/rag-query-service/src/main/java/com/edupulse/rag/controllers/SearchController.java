package com.edupulse.rag.controllers;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.edupulse.common.security.TokenClaims;
import com.edupulse.rag.dto.SearchRequestBody;
import com.edupulse.rag.dto.SearchResponse;
import com.edupulse.rag.services.SearchService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/search")
@RequiredArgsConstructor
class SearchController {

  private static final int DEFAULT_TOP_K = 5;

  private final SearchService searchService;

  /** POST so the question travels in the body, not in URLs that end up in access logs. */
  @PostMapping
  SearchResponse search(@Valid @RequestBody SearchRequestBody request,
      @AuthenticationPrincipal Jwt jwt) {
    int topK = request.topK() == null ? DEFAULT_TOP_K : request.topK();
    return searchService.search(request.query().trim(), topK, TokenClaims.orgUnit(jwt));
  }
}
