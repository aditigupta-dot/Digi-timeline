package com.digitaltimeline.user;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
@RestController @RequestMapping("/api/settings")
public class SettingsController {
  public record Update(@Size(min = 1, max = 80) String displayName, Boolean autoSync, Boolean aiExternalDefault) {}
  public record ChangePassword(@NotBlank String currentPassword, @Size(min = 8, max = 72) String newPassword) {}
  public record Confirm(@NotBlank String password) {}
  private final UserRepository users; private final UserService service;
  public SettingsController(UserRepository users, UserService service) { this.users = users; this.service = service; }
  @GetMapping UserRepository.Profile get(@AuthenticationPrincipal UUID u) { return users.get(u); }
  @PatchMapping UserRepository.Profile update(@AuthenticationPrincipal UUID u, @Valid @RequestBody Update r) {
    users.update(u, r.displayName() == null ? null : r.displayName().trim(), r.autoSync(), r.aiExternalDefault()); return users.get(u);
  }
  @PostMapping("/password") @ResponseStatus(HttpStatus.NO_CONTENT)
  void password(@AuthenticationPrincipal UUID u, @Valid @RequestBody ChangePassword r) { service.changePassword(u, r.currentPassword(), r.newPassword()); }
  @PostMapping("/sign-out-everywhere") @ResponseStatus(HttpStatus.NO_CONTENT)
  void signOutEverywhere(@AuthenticationPrincipal UUID u) { users.revokeAllRefresh(u); }
  @PostMapping("/delete-account") @ResponseStatus(HttpStatus.NO_CONTENT)
  void delete(@AuthenticationPrincipal UUID u, @Valid @RequestBody Confirm r) { service.deleteAccount(u, r.password()); }
}
