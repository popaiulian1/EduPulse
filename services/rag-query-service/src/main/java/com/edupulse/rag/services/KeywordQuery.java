package com.edupulse.rag.services;

import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;

/**
 * Turns a natural-language question into a PostgreSQL tsquery for the keyword half of the hybrid
 * search. The terms are OR-ed, because a question rarely contains every word of the passage that
 * answers it. Only letters and digits survive, so the result can never contain tsquery syntax.
 */
public final class KeywordQuery {

  private static final int MAX_TERMS = 20;

  private static final Set<String> STOPWORDS = Set.of(
      "the", "and", "for", "are", "was", "were", "with", "that", "this", "from", "have", "has",
      "had", "not", "but", "you", "your", "how", "what", "when", "where", "which", "who", "why",
      "can", "could", "should", "would", "does", "did", "into", "than", "then", "them", "they",
      "their", "there", "these", "those", "will", "its", "also", "any", "all", "been", "being",
      "about", "over", "such", "per", "via", "our", "out", "too", "may",
      // two-letter function words; "ai", "eu" and similar acronyms are kept on purpose
      "in", "of", "to", "is", "as", "at", "by", "on", "or", "an", "be", "it", "if", "do", "we",
      "no", "so", "up", "my", "me");

  private KeywordQuery() {
  }

  /** The tsquery text, or empty when nothing searchable is left (for example only stopwords). */
  public static Optional<String> toTsQuery(String text) {
    if (text == null) {
      return Optional.empty();
    }
    Set<String> terms = new LinkedHashSet<>();
    StringBuilder current = new StringBuilder();

    text.codePoints().forEach(codePoint -> {
      if (Character.isLetterOrDigit(codePoint)) {
        current.appendCodePoint(codePoint);
      } else {
        addTerm(terms, current);
      }
    });
    addTerm(terms, current);

    return terms.isEmpty() ? Optional.empty() : Optional.of(String.join(" | ", terms));
  }

  private static void addTerm(Set<String> terms, StringBuilder current) {
    if (current.length() >= 2 && terms.size() < MAX_TERMS) {
      String term = current.toString().toLowerCase(Locale.ROOT);
      if (!STOPWORDS.contains(term)) {
        terms.add(term);
      }
    }
    current.setLength(0);
  }
}
