package com.edupulse.rag.services;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Optional;

import org.junit.jupiter.api.Test;

class KeywordQueryTests {

  @Test
  void keepsTheMeaningfulWordsAndDropsStopwords() {
    assertEquals(Optional.of("hybrid | search | work | postgres"),
        KeywordQuery.toTsQuery("How does the hybrid search work in Postgres?"));
  }

  @Test
  void keepsShortAcronyms() {
    assertEquals(Optional.of("ai | act | eu"), KeywordQuery.toTsQuery("AI Act of the EU"));
  }

  @Test
  void keepsRegulatoryCodesAndNumbers() {
    assertEquals(Optional.of("iso | 27001 | 2022"), KeywordQuery.toTsQuery("ISO 27001:2022"));
  }

  @Test
  void dropsRepeatedTermsAndSingleLetters() {
    assertEquals(Optional.of("retention | records"),
        KeywordQuery.toTsQuery("a retention RETENTION of records"));
  }

  @Test
  void cannotSmuggleTsqueryOperators() {
    String query = KeywordQuery.toTsQuery("evil' & !(drop | table) <-> x:*").orElseThrow();
    assertEquals("evil | drop | table", query);
    assertTrue(query.matches("[\\p{L}\\p{N} |]+"), "only words and the OR separator");
  }

  @Test
  void keepsNonAsciiLetters() {
    assertEquals(Optional.of("școală | evaluări"),
        KeywordQuery.toTsQuery("Școală, evaluări!"));
  }

  @Test
  void returnsNothingForQueriesWithoutSearchableTerms() {
    assertEquals(Optional.empty(), KeywordQuery.toTsQuery("how are the and of"));
    assertEquals(Optional.empty(), KeywordQuery.toTsQuery("?! ... --"));
    assertEquals(Optional.empty(), KeywordQuery.toTsQuery(""));
    assertEquals(Optional.empty(), KeywordQuery.toTsQuery(null));
  }

  @Test
  void capsTheNumberOfTerms() {
    StringBuilder text = new StringBuilder();
    for (int i = 0; i < 100; i++) {
      text.append("term").append(i).append(' ');
    }
    String query = KeywordQuery.toTsQuery(text.toString()).orElseThrow();
    assertEquals(20, query.split(" \\| ").length);
  }
}
