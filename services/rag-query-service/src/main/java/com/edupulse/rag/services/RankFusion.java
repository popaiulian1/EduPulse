package com.edupulse.rag.services;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Reciprocal Rank Fusion: merges several ranked lists into one. An item scores the sum of
 * 1 / (k + rank) over every list it appears in, so items found by both searches rise to the top,
 * and the different score scales of vector similarity and text rank never have to be compared.
 */
public final class RankFusion {

  /** The constant from the original RRF paper; it damps the advantage of the very top ranks. */
  public static final int K = 60;

  private RankFusion() {
  }

  /** Items ordered by fused score, best first. Ties keep the order they were first seen in. */
  public static Map<String, Double> fuse(List<List<String>> rankedLists) {
    Map<String, Double> scores = new HashMap<>();
    List<String> firstSeen = new ArrayList<>();

    for (List<String> ranking : rankedLists) {
      for (int rank = 0; rank < ranking.size(); rank++) {
        String id = ranking.get(rank);
        if (!scores.containsKey(id)) {
          firstSeen.add(id);
        }
        scores.merge(id, 1.0 / (K + rank + 1), Double::sum);
      }
    }

    List<String> ordered = new ArrayList<>(firstSeen);
    ordered.sort((a, b) -> Double.compare(scores.get(b), scores.get(a))); // stable: ties keep order

    Map<String, Double> result = new LinkedHashMap<>();
    ordered.forEach(id -> result.put(id, scores.get(id)));
    return result;
  }
}
