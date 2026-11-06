package com.edupulse.common.logging;

import org.slf4j.MDC;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.util.ClassUtils;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.HandlerMapping;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

public class RequestContextInterceptor implements HandlerInterceptor {

  private static final boolean SECURITY_PRESENT = ClassUtils.isPresent(
      "org.springframework.security.core.context.SecurityContextHolder",
      RequestContextInterceptor.class.getClassLoader());

  @Override
  public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
    Object pattern = request.getAttribute(HandlerMapping.BEST_MATCHING_PATTERN_ATTRIBUTE);
    MDC.put(LogContext.ENDPOINT, EndpointNames.sanitize(pattern == null ? null : pattern.toString()));

    if (SECURITY_PRESENT) {
      String user = SecurityUserLookup.currentUser();
      if (user != null) {
        MDC.put(LogContext.USER, user);
      }
    }
    return true;
  }
}

final class SecurityUserLookup {
  private SecurityUserLookup() {
  }

  static String currentUser() {
    Authentication auth = SecurityContextHolder.getContext().getAuthentication();
    if (auth == null || !auth.isAuthenticated() || auth instanceof AnonymousAuthenticationToken) {
      return null;
    }
    return auth.getName();
  }
}
