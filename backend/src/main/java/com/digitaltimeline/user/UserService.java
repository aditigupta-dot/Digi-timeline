package com.digitaltimeline.user;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
/** Sensitive account actions. Both re-check the password. A wrong password is 403 (not 401) so the frontend does not treat it as an expired session. */
@Service
public class UserService {
  private final UserRepository repo; private final PasswordEncoder encoder;
  public UserService(UserRepository repo, PasswordEncoder encoder) { this.repo = repo; this.encoder = encoder; }
  @Transactional
  public void changePassword(UUID userId, String current, String next) {
    requirePassword(userId, current);
    repo.setPasswordHash(userId, encoder.encode(next));
    repo.revokeAllRefresh(userId); // every signed-in device must log in again
  }
  @Transactional
  public void deleteAccount(UUID userId, String password) { requirePassword(userId, password); repo.delete(userId); }
  private void requirePassword(UUID userId, String password) {
    if (!encoder.matches(password, repo.passwordHash(userId))) throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Password is incorrect");
  }
}
