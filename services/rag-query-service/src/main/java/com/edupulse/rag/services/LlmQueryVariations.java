package com.edupulse.rag.services;

import java.time.Duration;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.UnaryOperator;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import com.edupulse.rag.config.MultiQueryProperties;

import lombok.extern.slf4j.Slf4j;

/**
 * Query variations from the local Ollama chat model. The call is bounded by a timeout and any
 * failure just means "no variations": search carries on with the user's own question.
 *
 * Spring AI's MultiQueryExpander does the same job but only accepts a reply with exactly the
 * requested number of lines and otherwise returns the question unchanged. Small models often add
 * an introduction ("Here are three queries:"), so the reply is parsed leniently here instead:
 * {@link QuerySanitizer} removes the chatter and list markers and keeps the usable lines.
 */
@Slf4j
@Component
@ConditionalOnProperty(name = "edupulse.search.multi-query.enabled", havingValue = "true",
    matchIfMissing = true)
public class LlmQueryVariations implements QueryVariations {

  static final String PROMPT = """
      You help search compliance and policy documents. Rewrite the question below into \
      {number} different search queries that approach it from different angles: alternative \
      wording, synonyms, more specific terms, or the underlying requirement. Keep every query \
      short. Write exactly one query per line, with no numbering, no bullets, no quotes and \
      no other text.

      Question: {query}

      Queries:""";

  private final UnaryOperator<String> model;
  private final int variations;
  private final Duration timeout;
  private final Executor executor;

  @Autowired
  public LlmQueryVariations(ChatClient.Builder chatClientBuilder, MultiQueryProperties properties,
      @Qualifier("searchExecutor") Executor executor) {
    this(askModel(chatClientBuilder.build()), properties.variations(), properties.timeout(),
        executor);
  }

  /** @param model sends a prompt to the LLM and returns its reply */
  LlmQueryVariations(UnaryOperator<String> model, int variations, Duration timeout,
      Executor executor) {
    this.model = model;
    this.variations = variations;
    this.timeout = timeout;
    this.executor = executor;
  }

  private static UnaryOperator<String> askModel(ChatClient chatClient) {
    return prompt -> chatClient.prompt().user(prompt).call().content();
  }

  @Override
  public List<String> of(String query) {
    String prompt = PROMPT.replace("{number}", String.valueOf(variations)).replace("{query}", query);

    CompletableFuture<String> request = CompletableFuture.supplyAsync(() -> model.apply(prompt),
        executor);
    try {
      String reply = request.get(timeout.toMillis(), TimeUnit.MILLISECONDS);
      if (reply == null || reply.isBlank()) {
        return List.of();
      }
      List<String> lines = Arrays.asList(reply.split("\\R"));
      return QuerySanitizer.clean(query, lines, variations);
    } catch (TimeoutException e) {
      request.cancel(true);
      log.warn("Query expansion timed out after {} ms; searching with the original question only",
          timeout.toMillis());
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      log.warn("Query expansion interrupted");
    } catch (Exception e) {
      log.warn("Query expansion failed ({}); searching with the original question only",
          e.getCause() == null ? e.getMessage() : e.getCause().getMessage());
    }
    return List.of();
  }
}
