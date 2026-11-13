package com.digitaltimeline.ai;
import static org.junit.jupiter.api.Assertions.*;
import com.digitaltimeline.ai.AiProvider.Ref;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
class AiTests {
  @Test void classifiesTheExampleQuestions() {
    assertEquals(AiService.Intent.URGENT, AiService.classify("What are my most urgent tasks?", false));
    assertEquals(AiService.Intent.DEADLINES, AiService.classify("What deadlines are approaching?", false));
    assertEquals(AiService.Intent.TODAY, AiService.classify("Summarize my activity today", false));
    assertEquals(AiService.Intent.WEEK, AiService.classify("What did I work on this week?", false));
    assertEquals(AiService.Intent.PROJECT, AiService.classify("anything for Capstone?", true));
    assertEquals(AiService.Intent.SEARCH, AiService.classify("find invoices", false));
  }
  @Test void emptyContextProducesNoInventedContent() {
    String a = new RuleBasedProvider().answer("q", "label", List.of());
    assertTrue(a.contains("couldn't find")); assertFalse(a.contains("•"));
  }
  @Test void answerContainsOnlyRetrievedTitles() {
    var refs = List.of(new Ref("task", UUID.randomUUID(), "Finish report", null, Instant.parse("2026-11-14T09:00:00Z"), "Due within 24 hours"));
    String a = new RuleBasedProvider().answer("q", "Urgent", refs);
    assertTrue(a.contains("Finish report")); assertTrue(a.contains("Due within 24 hours"));
  }
}
