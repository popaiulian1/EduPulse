package com.edupulse.common.logging;

import java.util.Locale;

public final class EndpointNames {
  public static final String UNMATCHED = "_unmatched";
  private static final int MAX_LENGTH = 64;

  private EndpointNames() {
  }

  public static String sanitize(String pattern) {
    if (pattern == null || pattern.isBlank() || pattern.equals("/**")) {
      return UNMATCHED;
    }
    String name = trimSeparators(pattern.replaceAll("[^A-Za-z0-9._]+", "-"))
        .toLowerCase(Locale.ROOT);
    if (name.length() > MAX_LENGTH) {
      name = trimSeparators(name.substring(0, MAX_LENGTH));
    }
    return name.isEmpty() ? "root" : name;
  }

  /** Strips leading and trailing '.' and '-' (a plain scan, so no regex backtracking). */
  private static String trimSeparators(String value) {
    int start = 0;
    int end = value.length();
    while (start < end && isSeparator(value.charAt(start))) {
      start++;
    }
    while (end > start && isSeparator(value.charAt(end - 1))) {
      end--;
    }
    return value.substring(start, end);
  }

  private static boolean isSeparator(char c) {
    return c == '.' || c == '-';
  }
}
