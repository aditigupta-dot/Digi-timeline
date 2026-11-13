package com.digitaltimeline.auth;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
@RestController @RequestMapping("/api/auth")
public class AuthController {
  public record RegisterRequest(@Email @NotBlank String email, @NotBlank @Size(max = 80) String displayName,
                                @Size(min = 8, max = 72) String password) {}
  public record LoginRequest(@NotBlank String email, @NotBlank String password) {}
  public record RefreshRequest(@NotBlank String refreshToken) {}
  private final AuthService auth;
  public AuthController(AuthService auth) { this.auth = auth; }
  @PostMapping("/register") @ResponseStatus(HttpStatus.CREATED)
  AuthService.Tokens register(@Valid @RequestBody RegisterRequest r) { return auth.register(r.email(), r.displayName(), r.password()); }
  @PostMapping("/login") AuthService.Tokens login(@Valid @RequestBody LoginRequest r) { return auth.login(r.email(), r.password()); }
  @PostMapping("/refresh") AuthService.Tokens refresh(@Valid @RequestBody RefreshRequest r) { return auth.refresh(r.refreshToken()); }
  @PostMapping("/logout") @ResponseStatus(HttpStatus.NO_CONTENT)
  void logout(@Valid @RequestBody RefreshRequest r) { auth.logout(r.refreshToken()); }
}
