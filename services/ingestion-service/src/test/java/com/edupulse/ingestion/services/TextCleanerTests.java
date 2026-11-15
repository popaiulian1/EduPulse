package com.edupulse.ingestion.services;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class TextCleanerTests {

  @Test
  void collapsesRunsOfSpacesAndTabs() {
    assertEquals("Project Title: EduPulse AI",
        TextCleaner.clean("Project      Title:\t\t EduPulse          AI"));
  }

  @Test
  void treatsNonBreakingAndOtherUnicodeSpacesAsSpaces() {
    assertEquals("a b c", TextCleaner.clean("a  b c"));
  }

  @Test
  void dropsSpacesAtTheStartAndEndOfLines() {
    assertEquals("first line\nsecond line", TextCleaner.clean("first line   \n    second line  "));
  }

  @Test
  void normalisesLineBreaks() {
    assertEquals("a\nb\nc", TextCleaner.clean("a\r\nb\rc"));
  }

  @Test
  void keepsAtMostOneBlankLine() {
    assertEquals("a\n\nb", TextCleaner.clean("a\n\n\n\n\nb"));
    assertEquals("a\n\nb", TextCleaner.clean("a\n  \n \n\nb"));
  }

  @Test
  void trimsTheWholeText() {
    assertEquals("text", TextCleaner.clean("  \n\n  text \n \n"));
  }

  @Test
  void removesControlAndZeroWidthCharacters() {
    assertEquals("ab", TextCleaner.clean("a\u0000\u0007​﻿b"));
    assertEquals("cooperation", TextCleaner.clean("co­operation"));
  }

  @Test
  void leavesRegulatoryCodesAndNonAsciiLettersAlone() {
    assertEquals("ISO 27001:2022 Art. 5(1)(a) școală – § 4.2",
        TextCleaner.clean("ISO   27001:2022  Art. 5(1)(a)  școală – §  4.2"));
  }

  @Test
  void handlesEmptyAndNullInput() {
    assertEquals("", TextCleaner.clean(null));
    assertEquals("", TextCleaner.clean(""));
    assertEquals("", TextCleaner.clean(" \n\t  \n"));
  }

  @Test
  void isIdempotent() {
    String once = TextCleaner.clean("a   b \n\n\n c\t\td");
    assertEquals(once, TextCleaner.clean(once));
  }
}
