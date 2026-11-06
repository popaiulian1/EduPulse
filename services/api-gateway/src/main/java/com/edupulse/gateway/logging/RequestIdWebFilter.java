package com.edupulse.gateway.logging;

import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.cloud.gateway.route.Route;
import org.springframework.cloud.gateway.support.ServerWebExchangeUtils;
import org.springframework.core.Ordered;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;

import com.edupulse.common.logging.EndpointNames;
import com.edupulse.common.logging.LogContext;

import reactor.core.publisher.Mono;

@Component
public class RequestIdWebFilter implements WebFilter, Ordered {

  private static final Logger log = LoggerFactory.getLogger(RequestIdWebFilter.class);

  @Override
  public int getOrder() {
    return Ordered.HIGHEST_PRECEDENCE;
  }

  @Override
  public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
    String requestId = UUID.randomUUID().toString();

    ServerHttpRequest request = exchange.getRequest().mutate()
        .headers(headers -> headers.set(LogContext.REQUEST_ID_HEADER, requestId))
        .build();
    ServerWebExchange mutated = exchange.mutate().request(request).build();

    mutated.getAttributes().put(LogContext.REQUEST_ID, requestId);
    mutated.getResponse().beforeCommit(() -> {
      mutated.getResponse().getHeaders().set(LogContext.REQUEST_ID_HEADER, requestId);
      return Mono.empty();
    });

    long start = System.nanoTime();
    return chain.filter(mutated).doFinally(signal -> logAccess(mutated, requestId, start));
  }

  private void logAccess(ServerWebExchange exchange, String requestId, long startNanos) {
    String path = exchange.getRequest().getPath().value();
    if (path.startsWith("/actuator")) {
      return;
    }
    Route route = exchange.getAttribute(ServerWebExchangeUtils.GATEWAY_ROUTE_ATTR);
    String endpoint = EndpointNames.sanitize(route == null ? null : route.getId());
    String user = exchange.getAttribute(LogContext.USER);
    int status = exchange.getResponse().getStatusCode() == null
        ? 0
        : exchange.getResponse().getStatusCode().value();
    long millis = (System.nanoTime() - startNanos) / 1_000_000;

    MDC.put(LogContext.REQUEST_ID, requestId);
    MDC.put(LogContext.ENDPOINT, endpoint);
    if (user != null) {
      MDC.put(LogContext.USER, user);
    }
    try {
      log.info("{} {} -> {} ({} ms)", exchange.getRequest().getMethod(), path, status, millis);
    } finally {
      MDC.remove(LogContext.REQUEST_ID);
      MDC.remove(LogContext.ENDPOINT);
      MDC.remove(LogContext.USER);
    }
  }
}
