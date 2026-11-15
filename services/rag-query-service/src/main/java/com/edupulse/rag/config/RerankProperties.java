package com.edupulse.rag.config;

import java.time.Duration;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

/**
 * Reranking settings (edupulse.rerank.*).
 *
 * @param enabled    turn the reranking step off to search by fused rank only
 * @param url        base URL of the llama.cpp server running a cross-encoder with --reranking
 * @param model      model name sent along with each request
 * @param candidates how many fused candidates are reranked (the shortlist the cross-encoder sees)
 * @param minScore   results whose reranker relevance (0 to 1) is below this are dropped
 * @param timeout    connect and read timeout for the reranker call
 */
@ConfigurationProperties("edupulse.rerank")
public record RerankProperties(
    @DefaultValue("true") boolean enabled,
    @DefaultValue("http://localhost:8081") String url,
    @DefaultValue("bge-reranker-v2-m3") String model,
    @DefaultValue("20") int candidates,
    @DefaultValue("0.0") double minScore,
    @DefaultValue("10s") Duration timeout) {
}
