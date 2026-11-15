package com.edupulse.rag.config;

import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
class ExecutorConfig {

  /** Runs the searches of one question in parallel and bounds the LLM call. */
  @Bean(name = "searchExecutor", destroyMethod = "close")
  Executor searchExecutor() {
    ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor();
    return executor;
  }
}
