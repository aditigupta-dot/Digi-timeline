package com.digitaltimeline.sync;
import com.digitaltimeline.activity.ActivityRepository;
import com.digitaltimeline.activity.ImportedActivity;
import com.digitaltimeline.exception.UnauthorizedIntegrationException;
import com.digitaltimeline.integration.ActivityConnector;
import com.digitaltimeline.integration.IntegrationRepository;
import com.digitaltimeline.integration.Provider;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.concurrent.Executor;
import org.springframework.context.ApplicationEventPublisher;
import java.util.concurrent.RejectedExecutionException;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
/** Runs one connector in a background thread. A concurrent set guarantees one sync per (user, provider) at a time. */
@Service
public class SyncService {
  private static final Logger log = LoggerFactory.getLogger(SyncService.class);
  private final Map<Provider, ActivityConnector> connectors; private final IntegrationRepository integrations; private final ActivityRepository activities;
  private final Set<String> running = ConcurrentHashMap.newKeySet();
  private final Executor executor; private final ApplicationEventPublisher publisher;
  public SyncService(List<ActivityConnector> all, IntegrationRepository integrations, ActivityRepository activities, @Qualifier("syncExecutor") Executor executor, ApplicationEventPublisher publisher) {
    this.executor = executor; this.publisher = publisher;
    this.connectors = all.stream().collect(Collectors.toMap(ActivityConnector::provider, Function.identity())); // Factory-style lookup
    this.integrations = integrations; this.activities = activities;
  }
  public boolean supports(Provider p) { return connectors.containsKey(p); }
  public boolean isRunning(UUID userId, Provider p) { return running.contains(userId + ":" + p); }
  /** @return false if a sync for this user+provider is already running. */
  public boolean start(UUID userId, Provider p) {
    if (!running.add(userId + ":" + p)) return false;
    try { executor.execute(() -> execute(userId, p)); } // background thread from the pool
    catch (RejectedExecutionException e) { running.remove(userId + ":" + p); throw e; }
    return true;
  }
  public void execute(UUID userId, Provider p) {
    UUID integrationId = integrations.connectedId(userId, p).orElse(null);
    if (integrationId == null) { running.remove(userId + ":" + p); return; }
    UUID history = integrations.startHistory(integrationId);
    try {
      String startedAt = Instant.now().toString(); // next sync asks for items newer than this
      List<ImportedActivity> items = connectors.get(p).fetchNewActivities(userId, integrations.cursor(integrationId).orElse(null));
      int imported = activities.saveAll(userId, items);
      integrations.markSuccess(integrationId, history, startedAt, imported);
      publisher.publishEvent(new SyncFinishedEvent(userId, p, imported, true, null)); // Observer pattern: listeners react
    } catch (UnauthorizedIntegrationException e) {
      integrations.markError(integrationId, history, e.getMessage());
      publisher.publishEvent(new SyncFinishedEvent(userId, p, 0, false, e.getMessage()));
    } catch (Exception e) {
      log.warn("Sync failed for {}", p, e);
      String msg = e.getMessage() == null ? "Sync failed" : e.getMessage();
      integrations.markError(integrationId, history, msg);
      publisher.publishEvent(new SyncFinishedEvent(userId, p, 0, false, msg));
    } finally { running.remove(userId + ":" + p); }
  }
}
