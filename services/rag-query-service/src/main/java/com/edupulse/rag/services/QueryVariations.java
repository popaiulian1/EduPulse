package com.edupulse.rag.services;

import java.util.List;

/** Rewrites a question into other ways of asking it, to find passages worded differently. */
public interface QueryVariations {

  /**
   * @return rewritten queries, without the original and without duplicates; empty if none could
   *         be produced (this never fails the search)
   */
  List<String> of(String query);
}
