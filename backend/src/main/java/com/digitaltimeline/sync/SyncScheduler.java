package com.digitaltimeline.sync;
import com.digitaltimeline.integration.IntegrationRepository;
import com.digitaltimeline.integration.Provider;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
/** Periodic polling. Intervals come from application.yml (app.sync-intervals-ms.*). This only queues syncs; the thread pool does the work. */
@Component
@ConditionalOnProperty(name = "app.sync-enabled", havingValue = "true", matchIfMissing = true)
public class SyncScheduler {
  private static final Logger log = LoggerFactory.getLogger(SyncScheduler.class);
  private final IntegrationRepository integrations; private final SyncService sync;
  public SyncScheduler(IntegrationRepository integrations, SyncService sync) { this.integrations = integrations; this.sync = sync; }
  @Scheduled(initialDelay = 30000, fixedDelayString = "${app.sync-intervals-ms.github}") void github() { poll(Provider.GITHUB); }
  @Scheduled(initialDelay = 30000, fixedDelayString = "${app.sync-intervals-ms.gmail}") void gmail() { poll(Provider.GMAIL); }
  @Scheduled(initialDelay = 30000, fixedDelayString = "${app.sync-intervals-ms.calendar}") void calendar() { poll(Provider.CALENDAR); }
  @Scheduled(initialDelay = 30000, fixedDelayString = "${app.sync-intervals-ms.classroom}") void classroom() { poll(Provider.CLASSROOM); }
  private void poll(Provider p) {
    for (UUID user : integrations.connectedUserIds(p)) {
      try { sync.start(user, p); } // returns false if one is already running: safely skipped
      catch (RuntimeException e) { log.warn("Could not queue {} sync: {}", p, e.getMessage()); } // one user's problem never blocks others
    }
  }
}
