package com.edupulse.rag.services;

import java.net.http.HttpClient;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.MediaType;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import com.edupulse.rag.config.RerankProperties;

/**
 * Reranker backed by a llama.cpp server started with --reranking (the "reranker" container).
 * The server returns raw cross-encoder logits, which are mapped to 0..1 with a sigmoid so the
 * scores are readable and a threshold means the same thing for every query.
 */
@Component
@ConditionalOnProperty(name = "edupulse.rerank.enabled", havingValue = "true", matchIfMissing = true)
public class LlamaCppReranker implements Reranker {

  private static final ParameterizedTypeReference<Map<String, Object>> RESPONSE_TYPE =
      new ParameterizedTypeReference<>() {
      };

  private final RestClient client;
  private final String model;

  @Autowired
  public LlamaCppReranker(RerankProperties properties) {
    this(buildClient(properties), properties.model());
  }

  LlamaCppReranker(RestClient client, String model) {
    this.client = client;
    this.model = model;
  }

  private static RestClient buildClient(RerankProperties properties) {
    HttpClient httpClient = HttpClient.newBuilder().connectTimeout(properties.timeout()).build();
    JdkClientHttpRequestFactory requestFactory = new JdkClientHttpRequestFactory(httpClient);
    requestFactory.setReadTimeout(properties.timeout());
    return RestClient.builder().baseUrl(properties.url()).requestFactory(requestFactory).build();
  }

  @Override
  public double[] rerank(String query, List<String> passages) {
    if (passages.isEmpty()) {
      return new double[0];
    }

    Map<String, Object> body = Map.of(
        "model", model,
        "query", query,
        "top_n", passages.size(),
        "documents", passages);

    Map<String, Object> response;
    try {
      response = client.post()
          .uri("/v1/rerank")
          .contentType(MediaType.APPLICATION_JSON)
          .body(body)
          .retrieve()
          .body(RESPONSE_TYPE);
    } catch (RestClientException e) {
      throw new RerankException("The reranker could not be reached or returned an error", e);
    }
    return scores(response, passages.size());
  }

  /** The server returns results sorted by score, each pointing back to its input by index. */
  private static double[] scores(Map<String, Object> response, int expected) {
    if (response == null || !(response.get("results") instanceof List<?> results)) {
      throw new RerankException("The reranker response has no results");
    }
    double[] scores = new double[expected];
    boolean[] seen = new boolean[expected];

    for (Object item : results) {
      if (!(item instanceof Map<?, ?> result)
          || !(result.get("index") instanceof Number index)
          || !(result.get("relevance_score") instanceof Number score)) {
        throw new RerankException("The reranker response has an unexpected shape");
      }
      int position = index.intValue();
      if (position < 0 || position >= expected) {
        throw new RerankException("The reranker returned an index outside the request");
      }
      scores[position] = sigmoid(score.doubleValue());
      seen[position] = true;
    }
    for (boolean wasScored : seen) {
      if (!wasScored) {
        throw new RerankException("The reranker did not score every passage");
      }
    }
    return scores;
  }

  static double sigmoid(double logit) {
    return 1.0 / (1.0 + Math.exp(-logit));
  }
}
