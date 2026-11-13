package com.digitaltimeline.ai;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.util.Map;
import java.util.UUID;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
@RestController @RequestMapping("/api/ai")
public class AiController {
  public record ChatRequest(@NotBlank @Size(max = 500) String message, UUID conversationId, Boolean useExternalAi) {}
  private final AiService ai;
  public AiController(AiService ai) { this.ai = ai; }
  @PostMapping("/chat") AiService.Answer chat(@AuthenticationPrincipal UUID u, @Valid @RequestBody ChatRequest r) {
    return ai.chat(u, r.message().trim(), r.conversationId(), Boolean.TRUE.equals(r.useExternalAi()));
  }
  @GetMapping("/status") Map<String, Boolean> status() { return Map.of("externalAvailable", ai.externalAvailable()); }
}
