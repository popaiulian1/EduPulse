package com.edupulse.rag.services;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.jsonPath;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

class LlamaCppRerankerTests {

  private MockRestServiceServer server;
  private LlamaCppReranker reranker;

  @BeforeEach
  void setUp() {
    RestClient.Builder builder = RestClient.builder().baseUrl("http://reranker");
    server = MockRestServiceServer.bindTo(builder).build();
    reranker = new LlamaCppReranker(builder.build(), "test-model");
  }

  @Test
  void sendsTheQueryAndPassagesAndMapsScoresBackToTheInputOrder() {
    // the server answers best-first, each result pointing at its input by index
    server.expect(requestTo("http://reranker/v1/rerank"))
        .andExpect(method(HttpMethod.POST))
        .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
        .andExpect(jsonPath("$.model").value("test-model"))
        .andExpect(jsonPath("$.query").value("what is retained?"))
        .andExpect(jsonPath("$.documents.length()").value(3))
        .andExpect(jsonPath("$.top_n").value(3))
        .andRespond(withSuccess("""
            {"results":[
              {"index":2,"relevance_score":4.0},
              {"index":0,"relevance_score":0.0},
              {"index":1,"relevance_score":-6.0}]}
            """, MediaType.APPLICATION_JSON));

    double[] scores = reranker.rerank("what is retained?", List.of("a", "b", "c"));

    assertEquals(3, scores.length);
    assertEquals(0.5, scores[0], 1e-9);                    // sigmoid(0)
    assertEquals(LlamaCppReranker.sigmoid(-6.0), scores[1], 1e-9);
    assertEquals(LlamaCppReranker.sigmoid(4.0), scores[2], 1e-9);
    server.verify();
  }

  @Test
  void sigmoidMapsLogitsIntoZeroToOne() {
    assertEquals(0.5, LlamaCppReranker.sigmoid(0), 1e-12);
    assertEquals(1.0, LlamaCppReranker.sigmoid(50), 1e-12);
    assertEquals(0.0, LlamaCppReranker.sigmoid(-50), 1e-12);
  }

  @Test
  void doesNotCallTheServerForNoPassages() {
    assertArrayEquals(new double[0], reranker.rerank("anything", List.of()));
    server.verify();
  }

  @Test
  void aServerErrorBecomesARerankException() {
    server.expect(requestTo("http://reranker/v1/rerank")).andRespond(withServerError());

    assertThrows(RerankException.class, () -> reranker.rerank("q", List.of("a")));
  }

  @Test
  void aResponseWithoutResultsIsRejected() {
    server.expect(requestTo("http://reranker/v1/rerank"))
        .andRespond(withSuccess("{\"object\":\"list\"}", MediaType.APPLICATION_JSON));

    assertThrows(RerankException.class, () -> reranker.rerank("q", List.of("a")));
  }

  @Test
  void anIndexOutsideTheRequestIsRejected() {
    server.expect(requestTo("http://reranker/v1/rerank"))
        .andRespond(withSuccess("{\"results\":[{\"index\":5,\"relevance_score\":1.0}]}",
            MediaType.APPLICATION_JSON));

    assertThrows(RerankException.class, () -> reranker.rerank("q", List.of("a")));
  }

  @Test
  void aResponseThatSkipsAPassageIsRejected() {
    server.expect(requestTo("http://reranker/v1/rerank"))
        .andRespond(withSuccess("{\"results\":[{\"index\":0,\"relevance_score\":1.0}]}",
            MediaType.APPLICATION_JSON));

    assertThrows(RerankException.class, () -> reranker.rerank("q", List.of("a", "b")));
  }
}
