package com.digitaltimeline.sync;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import com.digitaltimeline.activity.ActivityRepository;
import com.digitaltimeline.activity.ImportedActivity;
import com.digitaltimeline.exception.UnauthorizedIntegrationException;
import com.digitaltimeline.integration.ActivityConnector;
import com.digitaltimeline.integration.IntegrationRepository;
import com.digitaltimeline.integration.Provider;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationEventPublisher;
class SyncServiceTest {
  private final UUID user = UUID.randomUUID(), integ = UUID.randomUUID(), hist = UUID.randomUUID();
  private final IntegrationRepository integrations = mock(IntegrationRepository.class);
  private final ActivityRepository activities = mock(ActivityRepository.class);
  private final ApplicationEventPublisher publisher = mock(ApplicationEventPublisher.class);
  private final List<Runnable> queue = new ArrayList<>(); // fake executor: we decide when queued work runs
  private SyncService service(ActivityConnector c) {
    when(integrations.connectedId(user, Provider.GITHUB)).thenReturn(Optional.of(integ));
    when(integrations.cursor(integ)).thenReturn(Optional.empty());
    when(integrations.startHistory(integ)).thenReturn(hist);
    return new SyncService(List.of(c), integrations, activities, queue::add, publisher);
  }
  private static ActivityConnector connector(java.util.function.Supplier<List<ImportedActivity>> body) {
    return new ActivityConnector() {
      public Provider provider() { return Provider.GITHUB; }
      public List<ImportedActivity> fetchNewActivities(UUID u, String cursor) { return body.get(); } };
  }
  @Test void secondStartIsRejectedUntilFirstFinishes() {
    when(activities.saveAll(eq(user), anyList())).thenReturn(2);
    var svc = service(connector(List::of));
    assertTrue(svc.start(user, Provider.GITHUB)); assertFalse(svc.start(user, Provider.GITHUB)); assertTrue(svc.isRunning(user, Provider.GITHUB));
    queue.get(0).run();
    verify(integrations).markSuccess(eq(integ), eq(hist), anyString(), eq(2));
    verify(publisher).publishEvent(any(SyncFinishedEvent.class));
    assertFalse(svc.isRunning(user, Provider.GITHUB)); assertTrue(svc.start(user, Provider.GITHUB));
  }
  @Test void revokedTokenMarksErrorAndStillReleasesTheLock() {
    var svc = service(connector(() -> { throw new UnauthorizedIntegrationException("Please reconnect."); }));
    svc.start(user, Provider.GITHUB); queue.get(0).run();
    verify(integrations).markError(integ, hist, "Please reconnect.");
    verify(publisher).publishEvent(any(SyncFinishedEvent.class)); assertFalse(svc.isRunning(user, Provider.GITHUB));
  }
}
