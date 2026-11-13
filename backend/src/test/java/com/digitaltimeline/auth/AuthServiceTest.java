package com.digitaltimeline.auth;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import com.digitaltimeline.config.AppProperties;
import com.digitaltimeline.security.JwtService;
import org.junit.jupiter.api.Test;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.web.server.ResponseStatusException;
class AuthServiceTest {
  @Test void sixthFailedLoginIsRateLimited() {
    var jdbc = mock(JdbcTemplate.class);
    when(jdbc.queryForMap(anyString(), any(Object[].class))).thenThrow(new EmptyResultDataAccessException(1)); // unknown email
    var svc = new AuthService(jdbc, new BCryptPasswordEncoder(), mock(JwtService.class), new AppProperties(new AppProperties.Jwt("x", 15, 7), "k", "http://x"));
    for (int i = 0; i < 5; i++) assertEquals(401, assertThrows(ResponseStatusException.class, () -> svc.login("a@b.com", "wrong")).getStatusCode().value());
    assertEquals(429, assertThrows(ResponseStatusException.class, () -> svc.login("a@b.com", "wrong")).getStatusCode().value());
  }
}
