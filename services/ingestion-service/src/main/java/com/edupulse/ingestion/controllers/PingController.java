package com.edupulse.ingestion.controllers;

import java.util.Map;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
class PingController {

  @GetMapping("/ping")
  Map<String, String> ping() {
    return Map.of("service", "ingestion-service", "status", "ok");
  }

}
