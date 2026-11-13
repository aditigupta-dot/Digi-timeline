package com.digitaltimeline.notification;
import com.digitaltimeline.sync.SyncFinishedEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
/** Observer: turns finished syncs into in-app notifications (only when something new arrived, or something went wrong). */
@Component
public class NotificationListener {
  private static final Logger log = LoggerFactory.getLogger(NotificationListener.class);
  private final NotificationRepository repo;
  public NotificationListener(NotificationRepository repo) { this.repo = repo; }
  @EventListener
  void on(SyncFinishedEvent e) {
    try {
      String name = e.provider().name().charAt(0) + e.provider().name().substring(1).toLowerCase();
      if (!e.success()) repo.add(e.userId(), name + " sync failed: " + e.error());
      else if (e.imported() > 0) repo.add(e.userId(), name + ": " + e.imported() + " new activit" + (e.imported() == 1 ? "y" : "ies") + " imported");
    } catch (RuntimeException ex) { log.warn("Could not save notification", ex); } // must never break the sync
  }
}
