package com.edupulse.rag.services;

import java.util.List;

/** Scores how well each passage answers a query, using a cross-encoder. */
public interface Reranker {

  /**
   * @return one relevance score per passage, in the same order as the input, each between 0
   *         (unrelated) and 1 (strongly relevant)
   * @throws RerankException if the reranker cannot be reached or answers with something unusable
   */
  double[] rerank(String query, List<String> passages);
}
