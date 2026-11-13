package com.digitaltimeline.integration;
import static org.junit.jupiter.api.Assertions.*;
import com.digitaltimeline.config.GoogleProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
class GoogleOAuthTest {
  private final GoogleOAuth oauth = new GoogleOAuth(new GoogleProperties("cid", "sec", "http://localhost:8080/api/integrations/{provider}/callback"), new ObjectMapper());
  @Test void gmailUrlUsesReadonlyScopeOfflineAndState() {
    String u = oauth.authorizeUrl(Provider.GMAIL, "abc");
    assertTrue(u.contains("gmail.readonly")); assertTrue(u.contains("access_type=offline")); assertTrue(u.contains("state=abc"));
    assertTrue(u.contains("integrations%2Fgmail%2Fcallback")); assertFalse(u.contains("sec"));
  }
  @Test void calendarUrlUsesEventsReadonlyScope() { assertTrue(oauth.authorizeUrl(Provider.CALENDAR, "s").contains("calendar.events.readonly")); }
  @Test void notConfiguredWhenCredentialsBlank() {
    assertFalse(new GoogleOAuth(new GoogleProperties("", "", "x"), new ObjectMapper()).configured()); assertTrue(oauth.configured()); }
}
