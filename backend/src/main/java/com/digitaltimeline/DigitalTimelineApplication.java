package com.digitaltimeline;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;
/** Entry point. Async + scheduling power background provider synchronization. */
@SpringBootApplication @EnableAsync @EnableScheduling
public class DigitalTimelineApplication {
  public static void main(String[] args) { SpringApplication.run(DigitalTimelineApplication.class, args); }
}
