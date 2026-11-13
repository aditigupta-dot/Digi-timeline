package com.digitaltimeline.config;
import java.util.concurrent.Executor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
/** Small thread pool for background syncs. Waits for running syncs on shutdown so stopping the app is graceful. */
@Configuration
public class AsyncConfig {
  @Bean("syncExecutor")
  Executor syncExecutor() {
    var ex = new ThreadPoolTaskExecutor();
    ex.setCorePoolSize(2); ex.setMaxPoolSize(4); ex.setQueueCapacity(20); ex.setThreadNamePrefix("sync-");
    ex.setWaitForTasksToCompleteOnShutdown(true); ex.setAwaitTerminationSeconds(20);
    return ex;
  }
}
