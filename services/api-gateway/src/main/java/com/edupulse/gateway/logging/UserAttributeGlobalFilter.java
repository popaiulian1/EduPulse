package com.edupulse.gateway.logging;

import java.security.Principal;

import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;

import com.edupulse.common.logging.LogContext;

import reactor.core.publisher.Mono;

@Component
public class UserAttributeGlobalFilter implements GlobalFilter, Ordered {

  @Override
  public int getOrder() {
    return Ordered.HIGHEST_PRECEDENCE;
  }

  @SuppressWarnings("null")
  @Override
  public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
    return exchange.getPrincipal()
        .map(Principal::getName)
        .doOnNext(name -> exchange.getAttributes().put(LogContext.USER, name))
        .then(Mono.defer(() -> chain.filter(exchange)));
  }
}
