package com.edupulse.rag.services;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.Executor;
import java.util.function.Supplier;

import org.slf4j.MDC;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import com.edupulse.rag.config.RerankProperties;
import com.edupulse.rag.dto.SearchResponse;
import com.edupulse.rag.dto.SearchResult;

import lombok.extern.slf4j.Slf4j;

/**
 * The retrieval pipeline:
 * <ol>
 * <li>the question is rewritten into a few variations by the LLM (optional);</li>
 * <li>every query runs a semantic and a keyword search in parallel, all restricted to the
 * caller's org unit;</li>
 * <li>all the rankings are fused, with one candidate per parent chunk;</li>
 * <li>the best candidates are reranked against the <em>original</em> question and weak matches
 * are dropped;</li>
 * <li>the parent chunk (the LLM context) is loaded for what is left.</li>
 * </ol>
 */
@Slf4j
@Service
public class SearchService {

  private static final int MIN_CANDIDATES = 20;
  private static final int CANDIDATES_PER_RESULT = 4;

  private final SemanticSearch semanticSearch;
  private final KeywordSearch keywordSearch;
  private final ParentLookup parentLookup;
  private final ObjectProvider<Reranker> rerankerProvider;
  private final ObjectProvider<QueryVariations> variationsProvider;
  private final RerankProperties rerankProperties;
  private final Executor executor;

  public SearchService(SemanticSearch semanticSearch, KeywordSearch keywordSearch,
      ParentLookup parentLookup, ObjectProvider<Reranker> rerankerProvider,
      ObjectProvider<QueryVariations> variationsProvider, RerankProperties rerankProperties,
      @Qualifier("searchExecutor") Executor executor) {
    this.semanticSearch = semanticSearch;
    this.keywordSearch = keywordSearch;
    this.parentLookup = parentLookup;
    this.rerankerProvider = rerankerProvider;
    this.variationsProvider = variationsProvider;
    this.rerankProperties = rerankProperties;
    this.executor = executor;
  }

  /** What one query found. */
  private record Run(List<ChunkHit> semantic, List<ChunkHit> keyword) {
    static final Run EMPTY = new Run(List.of(), List.of());
  }

  /** A parent's best child with its scores. */
  private record Candidate(ChunkHit hit, double fusedScore, List<String> matchedBy,
      Double rerankScore) {

    Candidate withRerankScore(double score) {
      return new Candidate(hit, fusedScore, matchedBy, score);
    }
  }

  public SearchResponse search(String query, int topK, String orgUnit) {
    int candidates = Math.max(MIN_CANDIDATES, topK * CANDIDATES_PER_RESULT);

    QueryVariations variationSource = variationsProvider.getIfAvailable();
    List<String> variations = variationSource == null ? List.of() : variationSource.of(query);

    List<String> queries = new ArrayList<>();
    queries.add(query);
    queries.addAll(variations);
    List<Run> runs = searchAll(queries, orgUnit, candidates);

    Reranker reranker = rerankerProvider.getIfAvailable();
    int shortlist = reranker == null ? topK : Math.max(topK, rerankProperties.candidates());
    List<Candidate> shortlisted = fuse(runs, shortlist);

    boolean reranked = false;
    if (reranker != null && !shortlisted.isEmpty()) {
      Optional<List<Candidate>> scored = rerank(reranker, query, shortlisted);
      if (scored.isPresent()) {
        shortlisted = scored.get().stream()
            .filter(c -> c.rerankScore() >= rerankProperties.minScore())
            .sorted(Comparator.comparingDouble(Candidate::rerankScore).reversed())
            .toList();
        reranked = true;
      }
    }

    List<Candidate> top = shortlisted.stream().limit(topK).toList();
    Map<String, String> parents = parentLookup.contents(
        top.stream().map(c -> c.hit().parentId()).toList(), orgUnit);

    List<SearchResult> results = top.stream().map(c -> new SearchResult(
        c.hit().documentId(), c.hit().documentName(), c.hit().page(),
        c.hit().content(),
        parents.getOrDefault(c.hit().parentId(), c.hit().content()),
        c.fusedScore(), c.hit().similarity(), c.rerankScore(), c.matchedBy())).toList();

    log.info("Search in {}: {} queries, {} candidates, reranked={}, {} results",
        orgUnit, queries.size(), shortlisted.size(), reranked, results.size());
    return new SearchResponse(query, variations, reranked, results);
  }

  /**
   * Runs the searches of every query in parallel. The user's own question must succeed; a failing
   * variation is skipped, because it only adds recall.
   */
  private List<Run> searchAll(List<String> queries, String orgUnit, int candidates) {
    List<CompletableFuture<Run>> futures = queries.stream()
        .map(q -> CompletableFuture.supplyAsync(
            withMdc(() -> searchOne(q, orgUnit, candidates)), executor))
        .toList();

    List<Run> runs = new ArrayList<>(futures.size());
    for (int i = 0; i < futures.size(); i++) {
      try {
        runs.add(futures.get(i).join());
      } catch (CompletionException e) {
        if (i == 0) {
          throw e.getCause() instanceof RuntimeException cause ? cause : e;
        }
        log.warn("Search for a query variation failed and was skipped: {}",
            e.getCause().getMessage());
        runs.add(Run.EMPTY);
      }
    }
    return runs;
  }

  private Run searchOne(String query, String orgUnit, int candidates) {
    List<ChunkHit> semantic = semanticSearch.search(query, orgUnit, candidates);
    List<ChunkHit> keyword = KeywordQuery.toTsQuery(query)
        .map(tsQuery -> keywordSearch.search(tsQuery, orgUnit, candidates))
        .orElse(List.of());
    return new Run(semantic, keyword);
  }

  /** Request-scoped log context (request id, user) does not follow work onto other threads. */
  private static <T> Supplier<T> withMdc(Supplier<T> task) {
    Map<String, String> context = MDC.getCopyOfContextMap();
    Thread caller = Thread.currentThread();
    return () -> {
      if (context == null || Thread.currentThread() == caller) {
        return task.get(); // same thread (or nothing to carry over): the MDC is already right
      }
      MDC.setContextMap(context);
      try {
        return task.get();
      } finally {
        MDC.clear();
      }
    };
  }

  /**
   * Fuses every ranking (each query's semantic and keyword list) and keeps the best child of each
   * parent, up to {@code limit}.
   */
  private static List<Candidate> fuse(List<Run> runs, int limit) {
    Map<String, ChunkHit> hitsById = new LinkedHashMap<>();
    Set<String> foundSemantically = new HashSet<>();
    Set<String> foundByKeyword = new HashSet<>();
    List<List<String>> rankings = new ArrayList<>();

    for (Run run : runs) {
      run.semantic().forEach(hit -> {
        hitsById.merge(hit.id(), hit, SearchService::moreSimilar);
        foundSemantically.add(hit.id());
      });
      run.keyword().forEach(hit -> {
        hitsById.merge(hit.id(), hit, SearchService::moreSimilar);
        foundByKeyword.add(hit.id());
      });
      rankings.add(run.semantic().stream().map(ChunkHit::id).toList());
      rankings.add(run.keyword().stream().map(ChunkHit::id).toList());
    }

    List<Candidate> candidates = new ArrayList<>();
    Set<String> seenParents = new HashSet<>();
    for (Map.Entry<String, Double> entry : RankFusion.fuse(rankings).entrySet()) {
      if (candidates.size() == limit) {
        break;
      }
      ChunkHit hit = hitsById.get(entry.getKey());
      String parentKey = Optional.ofNullable(hit.parentId()).orElse(hit.id());
      if (seenParents.add(parentKey)) {
        List<String> matchedBy = new ArrayList<>(2);
        if (foundSemantically.contains(hit.id())) {
          matchedBy.add("semantic");
        }
        if (foundByKeyword.contains(hit.id())) {
          matchedBy.add("keyword");
        }
        candidates.add(new Candidate(hit, entry.getValue(), matchedBy, null));
      }
    }
    return candidates;
  }

  /** The same chunk can be found by several queries; keep the copy with the best similarity. */
  private static ChunkHit moreSimilar(ChunkHit existing, ChunkHit other) {
    if (existing.similarity() == null) {
      return other.similarity() == null ? existing : other;
    }
    if (other.similarity() == null) {
      return existing;
    }
    return other.similarity() > existing.similarity() ? other : existing;
  }

  /** The cross-encoder reads each matched snippet. Empty if the reranker is not available. */
  private Optional<List<Candidate>> rerank(Reranker reranker, String query,
      List<Candidate> candidates) {
    try {
      double[] scores = reranker.rerank(query,
          candidates.stream().map(c -> c.hit().content()).toList());
      List<Candidate> scored = new ArrayList<>(candidates.size());
      for (int i = 0; i < candidates.size(); i++) {
        scored.add(candidates.get(i).withRerankScore(scores[i]));
      }
      return Optional.of(scored);
    } catch (RerankException e) {
      // search still works without it, just with the fused order and no relevance cut-off
      log.warn("Reranking skipped: {}", e.getMessage());
      return Optional.empty();
    }
  }
}
