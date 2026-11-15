package com.edupulse.ingestion.services;

/**
 * Cleans extracted text before chunking. PDF extraction pads text with runs of spaces and blank
 * lines, which waste tokens and add noise to the embeddings. This is a single pass over the
 * text, so it runs in linear time.
 *
 * Rules: runs of spaces, tabs and other space characters become one space; trailing and leading
 * spaces on a line are dropped; line breaks of any kind become "\n", and three or more in a row
 * become a blank line; control and zero-width characters are removed; the result is trimmed.
 */
public final class TextCleaner {

  private TextCleaner() {
  }

  public static String clean(String text) {
    if (text == null || text.isEmpty()) {
      return "";
    }
    StringBuilder out = new StringBuilder(text.length());
    boolean pendingSpace = false;
    int pendingNewlines = 0;

    for (int i = 0; i < text.length(); i++) {
      char c = text.charAt(i);

      if (isLineBreak(c)) {
        boolean partOfCrLf = c == '\r' && i + 1 < text.length() && text.charAt(i + 1) == '\n';
        if (!partOfCrLf && !out.isEmpty()) {
          pendingNewlines++;
        }
        pendingSpace = false;
      } else if (isSpace(c)) {
        pendingSpace = true;
      } else if (isDropped(c)) {
        continue;
      } else {
        if (!out.isEmpty()) {
          if (pendingNewlines > 0) {
            out.append(pendingNewlines == 1 ? "\n" : "\n\n");
          } else if (pendingSpace) {
            out.append(' ');
          }
        }
        out.append(c);
        pendingSpace = false;
        pendingNewlines = 0;
      }
    }
    return out.toString();
  }

  private static boolean isLineBreak(char c) {
    return c == '\n' || c == '\r' || c == '\f' || c == '\u000B'
        || c == '\u0085' || c == ' ' || c == ' ';
  }

  private static boolean isSpace(char c) {
    return c == '\t' || Character.isSpaceChar(c);
  }

  private static boolean isDropped(char c) {
    return Character.isISOControl(c) || c == '​' || c == '﻿' || c == '­';
  }
}
