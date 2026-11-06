package com.edupulse.auth.controllers;

import java.util.Map;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestController
class PingController {

  @GetMapping("/ping")
  Map<String, String> ping() {
    log.info("Ping requested");
    return Map.of("service", "auth-service", "status", "ok");
  }

}
