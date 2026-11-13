package com.digitaltimeline.user;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.web.server.ResponseStatusException;
class UserServiceTest {
  private final BCryptPasswordEncoder enc = new BCryptPasswordEncoder(); private final UserRepository repo = mock(UserRepository.class);
  private final UUID id = UUID.randomUUID(); private final UserService svc = new UserService(repo, enc);
  UserServiceTest() { when(repo.passwordHash(id)).thenReturn(enc.encode("old-password1")); }
  @Test void wrongCurrentPasswordIsForbiddenAndChangesNothing() {
    var e = assertThrows(ResponseStatusException.class, () -> svc.changePassword(id, "nope", "new-password1"));
    assertEquals(403, e.getStatusCode().value()); verify(repo, never()).setPasswordHash(any(), any());
  }
  @Test void correctPasswordStoresHashAndRevokesSessions() {
    svc.changePassword(id, "old-password1", "new-password1");
    verify(repo).setPasswordHash(eq(id), argThat(h -> enc.matches("new-password1", h))); verify(repo).revokeAllRefresh(id);
  }
  @Test void deleteNeedsCorrectPassword() {
    assertThrows(ResponseStatusException.class, () -> svc.deleteAccount(id, "bad")); verify(repo, never()).delete(any());
    svc.deleteAccount(id, "old-password1"); verify(repo).delete(id);
  }
}
