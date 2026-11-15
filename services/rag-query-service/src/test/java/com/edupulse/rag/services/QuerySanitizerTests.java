package com.edupulse.rag.services;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.Test;

class QuerySanitizerTests {

  private static final String ORIGINAL = "How long do we keep records?";

  @Test
  void stripsListMarkersAndQuotes() {
    assertEquals(List.of("record retention period", "data storage duration", "archive policy"),
        QuerySanitizer.clean(ORIGINAL, List.of(
            "1. record retention period",
            "- \"data storage duration\"",
            "• archive policy"), 5));
  }

  @Test
  void dropsTheOriginalQuestionAndDuplicatesIgnoringCaseAndSpacing() {
    assertEquals(List.of("record retention"),
        QuerySanitizer.clean(ORIGINAL, List.of(
            "how long do we   KEEP records?",
            "record retention",
            "Record  Retention"), 5));
  }

  @Test
  void dropsIntroductoryLinesAndBlankLines() {
    assertEquals(List.of("retention schedule"),
        QuerySanitizer.clean(ORIGINAL, List.of(
            "Here are three different versions of the query:",
            "",
            "   ",
            "retention schedule"), 5));
  }

  @Test
  void dropsRamblingLines() {
    String rambling = "word ".repeat(100);

    assertTrue(QuerySanitizer.clean(ORIGINAL, List.of(rambling), 5).isEmpty());
  }

  @Test
  void keepsAtMostTheRequestedNumber() {
    assertEquals(List.of("a one", "b two"),
        QuerySanitizer.clean(ORIGINAL, List.of("a one", "b two", "c three", "d four"), 2));
  }

  @Test
  void keepsNumbersThatAreNotListMarkers() {
    assertEquals(List.of("ISO 27001 retention rules", "2022 audit period"),
        QuerySanitizer.clean(ORIGINAL, List.of("ISO 27001 retention rules", "2022 audit period"), 5));
  }

  @Test
  void toleratesNullsAndEmptyInput() {
    assertTrue(QuerySanitizer.clean(ORIGINAL, List.of(), 3).isEmpty());
    assertTrue(QuerySanitizer.clean(ORIGINAL, Arrays.asList(null, " "), 3).isEmpty());
  }
}
