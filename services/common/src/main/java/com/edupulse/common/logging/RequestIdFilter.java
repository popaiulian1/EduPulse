package com.edupulse.common.logging;

import java.io.IOException;
import java.util.UUID;
import java.util.regex.Pattern;

import org.slf4j.MDC;
import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

public class RequestIdFilter extends OncePerRequestFilter {

  private static final Pattern VALID_ID = Pattern.compile("^[A-Za-z0-9_-]{8,64}$");

  @Override
  protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
      FilterChain chain) throws ServletException, IOException {
    String incoming = request.getHeader(LogContext.REQUEST_ID_HEADER);
    String requestId = (incoming != null && VALID_ID.matcher(incoming).matches())
        ? incoming
        : UUID.randomUUID().toString();

    MDC.put(LogContext.REQUEST_ID, requestId);
    response.setHeader(LogContext.REQUEST_ID_HEADER, requestId);
    try {
      chain.doFilter(request, response);
    } finally {
      MDC.remove(LogContext.REQUEST_ID);
      MDC.remove(LogContext.ENDPOINT);
      MDC.remove(LogContext.USER);
    }
  }
}
