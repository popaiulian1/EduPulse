package com.edupulse.rag.services;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Duration;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;

import com.edupulse.rag.config.RerankProperties;
import com.edupulse.rag.dto.SearchResponse;
import com.edupulse.rag.dto.SearchResult;

class SearchServiceTests {

  private static final String ORG = "SCHOOL_1";

  private SemanticSearch semantic;
  private KeywordSearch keyword;
  private ParentLookup parents;
  private Reranker reranker;
  private ObjectProvider<Reranker> rerankerProvider;
  private ObjectProvider<QueryVariations> variationsProvider;
  private QueryVariations variations;

  @BeforeEach
  @SuppressWarnings("unchecked")
  void setUp() {
    semantic = mock(SemanticSearch.class);
    keyword = mock(KeywordSearch.class);
    parents = mock(ParentLookup.class);
    reranker = mock(Reranker.class);
    rerankerProvider = mock(ObjectProvider.class);
    variationsProvider = mock(ObjectProvider.class);
    variations = mock(QueryVariations.class);
    when(rerankerProvider.getIfAvailable()).thenReturn(reranker);
    when(keyword.search(anyString(), eq(ORG), anyInt())).thenReturn(List.of());
    when(parents.contents(any(), eq(ORG))).thenReturn(
        Map.of("p1", "context 1", "p2", "context 2", "p3", "context 3"));
  }

  private SearchService service(double minScore) {
    RerankProperties properties =
        new RerankProperties(true, "http://reranker", "m", 20, minScore, Duration.ofSeconds(1));
    return new SearchService(semantic, keyword, parents, rerankerProvider, variationsProvider,
        properties, Runnable::run);
  }

  private static ChunkHit hit(String id, String parent) {
    return new ChunkHit(id, "text " + id, "doc-" + parent, "policy.pdf", 3, parent, 0.5);
  }

  @Test
  void rerankingReordersTheResultsAndDropsWeakMatches() {
    when(semantic.search(anyString(), eq(ORG), anyInt()))
        .thenReturn(List.of(hit("c1", "p1"), hit("c2", "p2"), hit("c3", "p3")));
    when(reranker.rerank(anyString(), any())).thenReturn(new double[] { 0.10, 0.90, 0.50 });

    SearchResponse response = service(0.2).search("retention period", 5, ORG);

    assertTrue(response.reranked());
    List<SearchResult> results = response.results();
    assertEquals(2, results.size(), "the 0.10 match is below the 0.2 threshold");
    assertEquals("text c2", results.get(0).snippet());
    assertEquals(0.90, results.get(0).rerankScore());
    assertEquals("text c3", results.get(1).snippet());
    assertEquals("context 2", results.get(0).context());
  }

  @Test
  void theRerankerSeesTheSnippetsOfTheFusedShortlist() {
    when(semantic.search(anyString(), eq(ORG), anyInt()))
        .thenReturn(List.of(hit("c1", "p1"), hit("c2", "p2")));
    when(reranker.rerank(anyString(), any())).thenReturn(new double[] { 0.5, 0.5 });

    service(0.0).search("retention period", 5, ORG);

    verify(reranker).rerank("retention period", List.of("text c1", "text c2"));
  }

  @Test
  void fallsBackToTheFusedOrderWhenTheRerankerFails() {
    when(semantic.search(anyString(), eq(ORG), anyInt()))
        .thenReturn(List.of(hit("c1", "p1"), hit("c2", "p2")));
    when(reranker.rerank(anyString(), any())).thenThrow(new RerankException("down"));

    SearchResponse response = service(0.9).search("retention period", 5, ORG);

    assertFalse(response.reranked());
    assertEquals(2, response.results().size(), "no threshold is applied without scores");
    assertEquals("text c1", response.results().get(0).snippet());
    assertNull(response.results().get(0).rerankScore());
  }

  @Test
  void worksWithoutAnyRerankerConfigured() {
    when(rerankerProvider.getIfAvailable()).thenReturn(null);
    when(semantic.search(anyString(), eq(ORG), anyInt()))
        .thenReturn(List.of(hit("c1", "p1"), hit("c2", "p2"), hit("c3", "p3")));

    SearchResponse response = service(0.5).search("retention period", 2, ORG);

    assertFalse(response.reranked());
    assertEquals(2, response.results().size(), "limited to topK");
    verify(reranker, never()).rerank(anyString(), any());
  }

  @Test
  void returnsOneResultPerParentChunk() {
    when(semantic.search(anyString(), eq(ORG), anyInt()))
        .thenReturn(List.of(hit("c1", "p1"), hit("c2", "p1"), hit("c3", "p2")));
    when(reranker.rerank(anyString(), any())).thenReturn(new double[] { 0.8, 0.6 });

    SearchResponse response = service(0.0).search("retention period", 5, ORG);

    assertEquals(2, response.results().size());
    verify(reranker).rerank("retention period", List.of("text c1", "text c3"));
  }

  @Test
  void anEverythingBelowTheThresholdAnswerIsEmptyButStillMarkedReranked() {
    when(semantic.search(anyString(), eq(ORG), anyInt()))
        .thenReturn(List.of(hit("c1", "p1"), hit("c2", "p2")));
    when(reranker.rerank(anyString(), any())).thenReturn(new double[] { 0.01, 0.02 });

    SearchResponse response = service(0.5).search("unrelated question", 5, ORG);

    assertTrue(response.reranked());
    assertTrue(response.results().isEmpty());
  }

  @Test
  void keywordsOnlyMatchedChunksAreMarkedAndKeptWhenSemanticFindsNothing() {
    when(semantic.search(anyString(), eq(ORG), anyInt())).thenReturn(List.of());
    ChunkHit codeHit = new ChunkHit("k1", "XQ-7741 text", "doc", "policy.txt", 1, "p1", null);
    when(keyword.search(eq("xq | 7741"), eq(ORG), anyInt())).thenReturn(List.of(codeHit));
    when(reranker.rerank(anyString(), any())).thenReturn(new double[] { 0.7 });

    SearchResponse response = service(0.0).search("XQ-7741", 5, ORG);

    assertEquals(1, response.results().size());
    assertEquals(List.of("keyword"), response.results().get(0).matchedBy());
    assertNull(response.results().get(0).similarity());
  }

  @Test
  void passagesFoundOnlyThroughAVariationAreIncludedAndTheOriginalQuestionIsReranked() {
    when(variationsProvider.getIfAvailable()).thenReturn(variations);
    when(variations.of("retention period")).thenReturn(List.of("how long records are kept"));
    when(semantic.search(eq("retention period"), eq(ORG), anyInt()))
        .thenReturn(List.of(hit("c1", "p1")));
    when(semantic.search(eq("how long records are kept"), eq(ORG), anyInt()))
        .thenReturn(List.of(hit("c2", "p2")));
    when(reranker.rerank(anyString(), any())).thenReturn(new double[] { 0.6, 0.9 });

    SearchResponse response = service(0.0).search("retention period", 5, ORG);

    assertEquals(List.of("how long records are kept"), response.expandedQueries());
    assertEquals(2, response.results().size());
    assertEquals("text c2", response.results().get(0).snippet());
    // relevance is judged against what the user asked, not against a rewrite
    verify(reranker).rerank("retention period", List.of("text c1", "text c2"));
  }

  @Test
  void aChunkFoundByTheQuestionAndByAVariationRanksAboveChunksFoundOnce() {
    when(rerankerProvider.getIfAvailable()).thenReturn(null);
    when(variationsProvider.getIfAvailable()).thenReturn(variations);
    when(variations.of("retention period")).thenReturn(List.of("record keeping"));
    when(semantic.search(eq("retention period"), eq(ORG), anyInt()))
        .thenReturn(List.of(hit("c1", "p1"), hit("c2", "p2")));
    when(semantic.search(eq("record keeping"), eq(ORG), anyInt()))
        .thenReturn(List.of(hit("c2", "p2")));

    SearchResponse response = service(0.0).search("retention period", 5, ORG);

    assertEquals("text c2", response.results().get(0).snippet());
    assertEquals("text c1", response.results().get(1).snippet());
  }

  @Test
  void aFailingVariationSearchIsSkipped() {
    when(variationsProvider.getIfAvailable()).thenReturn(variations);
    when(variations.of("retention period")).thenReturn(List.of("record keeping"));
    when(semantic.search(eq("retention period"), eq(ORG), anyInt()))
        .thenReturn(List.of(hit("c1", "p1")));
    when(semantic.search(eq("record keeping"), eq(ORG), anyInt()))
        .thenThrow(new IllegalStateException("embedding model busy"));
    when(reranker.rerank(anyString(), any())).thenReturn(new double[] { 0.8 });

    SearchResponse response = service(0.0).search("retention period", 5, ORG);

    assertEquals(1, response.results().size());
    assertEquals("text c1", response.results().get(0).snippet());
  }

  @Test
  void aFailingSearchForTheUsersOwnQuestionIsNotHidden() {
    when(semantic.search(anyString(), eq(ORG), anyInt()))
        .thenThrow(new IllegalStateException("database down"));

    assertThrows(IllegalStateException.class,
        () -> service(0.0).search("retention period", 5, ORG));
  }

  @Test
  void noVariationsAreReportedWhenExpansionIsNotConfigured() {
    when(semantic.search(anyString(), eq(ORG), anyInt())).thenReturn(List.of(hit("c1", "p1")));
    when(reranker.rerank(anyString(), any())).thenReturn(new double[] { 0.8 });

    SearchResponse response = service(0.0).search("retention period", 5, ORG);

    assertTrue(response.expandedQueries().isEmpty());
  }

  @Test
  void skipsTheKeywordSearchForQuestionsWithoutSearchableTerms() {
    when(semantic.search(anyString(), eq(ORG), anyInt())).thenReturn(List.of());

    service(0.0).search("how are the and of", 5, ORG);

    verify(keyword, never()).search(anyString(), anyString(), anyInt());
  }

  @Test
  void doesNotCallTheRerankerWhenNothingWasFound() {
    when(semantic.search(anyString(), eq(ORG), anyInt())).thenReturn(List.of());

    SearchResponse response = service(0.0).search("retention period", 5, ORG);

    assertTrue(response.results().isEmpty());
    assertFalse(response.reranked());
    verify(reranker, never()).rerank(anyString(), any());
  }
}
