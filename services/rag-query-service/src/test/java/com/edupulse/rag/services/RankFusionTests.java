package com.edupulse.rag.services;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

class RankFusionTests {

  @Test
  void itemsFoundByBothSearchesRankFirst() {
    Map<String, Double> fused = RankFusion.fuse(List.of(
        List.of("a", "b", "c"),
        List.of("c", "d")));

    // c is in both lists; b and d tie (both second in their list), and b was seen first
    assertEquals(List.of("c", "a", "b", "d"), List.copyOf(fused.keySet()));
  }

  @Test
  void scoreIsTheSumOfReciprocalRanks() {
    Map<String, Double> fused = RankFusion.fuse(List.of(List.of("x"), List.of("y", "x")));

    assertEquals(1.0 / 61 + 1.0 / 62, fused.get("x"), 1e-12);
    assertEquals(1.0 / 61, fused.get("y"), 1e-12);
  }

  @Test
  void tiesKeepTheOrderTheyWereFirstSeenIn() {
    Map<String, Double> fused = RankFusion.fuse(List.of(List.of("first"), List.of("second")));

    assertEquals(List.of("first", "second"), List.copyOf(fused.keySet()));
  }

  @Test
  void worksWhenOneListIsEmpty() {
    Map<String, Double> fused = RankFusion.fuse(List.of(List.of("a", "b"), List.of()));

    assertEquals(List.of("a", "b"), List.copyOf(fused.keySet()));
    assertTrue(fused.get("a") > fused.get("b"));
  }

  @Test
  void worksWhenBothListsAreEmpty() {
    assertTrue(RankFusion.fuse(List.of(List.of(), List.of())).isEmpty());
  }
}
