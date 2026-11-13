package com.digitaltimeline.integration;
import com.digitaltimeline.activity.ImportedActivity;
import java.util.List;
import java.util.UUID;
/** Strategy interface: one implementation per provider (GitHub, Gmail, Calendar, Classroom). */
public interface ActivityConnector {
  Provider provider();
  /** Fetch new activities using the user's stored OAuth tokens. Throws SyncException on failure. */
  List<ImportedActivity> fetchNewActivities(UUID userId, String cursor);
}
