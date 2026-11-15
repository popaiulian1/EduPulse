package com.edupulse.rag.services;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Duration;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.UnaryOperator;

import org.junit.jupiter.api.Test;

class LlmQueryVariationsTests {

  private static LlmQueryVariations variations(UnaryOperator<String> model, Duration timeout) {
    return new LlmQueryVariations(model, 3, timeout, Executors.newVirtualThreadPerTaskExecutor());
  }

  @Test
  void parsesAReplyThatStartsWithAnIntroductionAndNumbering() {
    UnaryOperator<String> model = prompt -> """
        Here are three short search queries:

        1. How long can companies hold customer data?
        2. customer data retention periods
        3. "what is the average retention period"
        """;

    assertEquals(List.of("How long can companies hold customer data?",
        "customer data retention periods", "what is the average retention period"),
        variations(model, Duration.ofSeconds(5)).of("How long must we keep old customer data?"));
  }

  @Test
  void parsesAPlainOneQueryPerLineReply() {
    UnaryOperator<String> model = prompt -> "record retention period\r\ndata storage duration\r\n";

    assertEquals(List.of("record retention period", "data storage duration"),
        variations(model, Duration.ofSeconds(5)).of("How long do we keep records?"));
  }

  @Test
  void dropsTheOriginalQuestionAndKeepsAtMostTheRequestedNumber() {
    UnaryOperator<String> model = prompt -> "how long do we keep records?\na one\nb two\nc three\nd four";

    assertEquals(List.of("a one", "b two", "c three"),
        variations(model, Duration.ofSeconds(5)).of("How long do we keep records?"));
  }

  @Test
  void theQuestionAndTheRequestedCountAreFilledIntoThePrompt() {
    AtomicReference<String> seen = new AtomicReference<>();
    UnaryOperator<String> model = prompt -> {
      seen.set(prompt);
      return "";
    };

    variations(model, Duration.ofSeconds(5)).of("What is the retention period?");

    assertTrue(seen.get().contains("Question: What is the retention period?"));
    assertTrue(seen.get().contains("into 3 different search queries"));
    assertTrue(!seen.get().contains("{number}") && !seen.get().contains("{query}"));
  }

  @Test
  void aQuestionContainingPlaceholdersIsNotExpandedAgain() {
    AtomicReference<String> seen = new AtomicReference<>();
    UnaryOperator<String> model = prompt -> {
      seen.set(prompt);
      return "";
    };

    variations(model, Duration.ofSeconds(5)).of("what does {number} mean");

    assertTrue(seen.get().contains("Question: what does {number} mean"));
  }

  @Test
  void anEmptyReplyMeansNoVariations() {
    assertTrue(variations(prompt -> "  \n ", Duration.ofSeconds(5)).of("question").isEmpty());
    assertTrue(variations(prompt -> null, Duration.ofSeconds(5)).of("question").isEmpty());
  }

  @Test
  void anErrorFromTheModelMeansNoVariations() {
    UnaryOperator<String> model = prompt -> {
      throw new IllegalStateException("model not available");
    };

    assertTrue(variations(model, Duration.ofSeconds(5)).of("question").isEmpty());
  }

  @Test
  void aSlowModelIsAbandonedAfterTheTimeout() {
    CountDownLatch release = new CountDownLatch(1);
    UnaryOperator<String> model = prompt -> {
      try {
        release.await();
      } catch (InterruptedException e) {
        Thread.currentThread().interrupt();
      }
      return "too late";
    };

    long start = System.nanoTime();
    List<String> result = variations(model, Duration.ofMillis(200)).of("question");
    long millis = (System.nanoTime() - start) / 1_000_000;
    release.countDown();

    assertTrue(result.isEmpty());
    assertTrue(millis < 3000, "should give up after about 200 ms, took " + millis);
  }
}
