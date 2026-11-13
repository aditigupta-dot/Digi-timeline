package com.digitaltimeline.config;
import org.springframework.boot.context.properties.ConfigurationProperties;
/** Typed view of the `app.*` section in application.yml (secrets come from env vars). */
@ConfigurationProperties("app")
public record AppProperties(Jwt jwt, String tokenEncryptionKey, String corsOrigin) {
  public record Jwt(String secret, int accessMinutes, int refreshDays) {}
}
