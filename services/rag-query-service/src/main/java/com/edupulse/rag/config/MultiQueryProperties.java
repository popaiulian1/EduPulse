package com.edupulse.rag.config;

import java.time.Duration;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

/**
 * Multi-query expansion settings (edupulse.search.multi-query.*).
 *
 * @param enabled    turn expansion off to search with the user's question only
 * @param variations how many rewritten queries the LLM is asked for
 * @param timeout    how long search waits for the LLM before using the original question alone
 */
@ConfigurationProperties("edupulse.search.multi-query")
public record MultiQueryProperties(
    @DefaultValue("true") boolean enabled,
    @DefaultValue("3") int variations,
    @DefaultValue("3s") Duration timeout) {
}
