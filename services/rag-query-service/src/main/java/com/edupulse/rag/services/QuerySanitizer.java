package com.edupulse.rag.services;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * Cleans what a small LLM returns when asked for query rewrites. Models add list markers,
 * quotes, an introductory sentence or the original question itself; none of that should reach
 * the search.
 */
public final class QuerySanitizer {

  private static final int MAX_LENGTH = 300;
  private static final Pattern LIST_MARKER = Pattern.compile("^(?:\\d{1,2}[.)]|[-*•])\\s+");

  private QuerySanitizer() {
  }

  /** Cleaned, distinct rewrites that differ from the original, at most {@code max}. */
  public static List<String> clean(String original, List<String> raw, int max) {
    Set<String> seen = new HashSet<>();
    seen.add(normalise(original));
    List<String> cleaned = new ArrayList<>();

    for (String line : raw) {
      String query = tidy(line);
      if (!query.isEmpty() && !isChatter(query) && seen.add(normalise(query))) {
        cleaned.add(query);
        if (cleaned.size() == max) {
          break;
        }
      }
    }
    return cleaned;
  }

  private static String tidy(String line) {
    if (line == null) {
      return "";
    }
    String query = LIST_MARKER.matcher(line.strip()).replaceFirst("");
    query = stripQuotes(query.strip());
    return query.replaceAll("\\s+", " ").strip();
  }

  private static String stripQuotes(String text) {
    if (text.length() >= 2) {
      char first = text.charAt(0);
      char last = text.charAt(text.length() - 1);
      boolean quoted = (first == '"' && last == '"') || (first == '\'' && last == '\'')
          || (first == '“' && last == '”');
      if (quoted) {
        return text.substring(1, text.length() - 1);
      }
    }
    return text;
  }

  /** Introductions like "Here are three versions:" and rambling lines are not queries. */
  private static boolean isChatter(String query) {
    return query.length() > MAX_LENGTH || query.endsWith(":");
  }

  private static String normalise(String query) {
    return query.strip().replaceAll("\\s+", " ").toLowerCase(Locale.ROOT);
  }
}
