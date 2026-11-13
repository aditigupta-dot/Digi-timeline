package com.digitaltimeline.sync;
import com.digitaltimeline.integration.Provider;
import java.util.UUID;
/** Published after every sync attempt. Notifications and live updates subscribe to it; SyncService knows nothing about them. */
public record SyncFinishedEvent(UUID userId, Provider provider, int imported, boolean success, String error) {}
