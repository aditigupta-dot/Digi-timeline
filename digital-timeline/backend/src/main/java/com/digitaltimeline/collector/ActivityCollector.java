package com.digitaltimeline.collector;

import com.digitaltimeline.domain.User;
import com.digitaltimeline.dto.ActivityRequest;
import com.digitaltimeline.exception.IngestionException;
import com.digitaltimeline.service.ActivityService;
import java.util.List;
import java.util.Map;

/**
 * TEMPLATE METHOD pattern: collect() fixes the algorithm (validate -> fetch -> save with de-duplication -> report);
 * subclasses only provide the source-specific parts. Adding a new data source = one new subclass.
 */
public abstract class ActivityCollector {

    public record CollectedItem(String externalId, ActivityRequest request) {}

    public record CollectionResult(String source, int fetched, int saved, int skipped, long millis, String error) {
        public static CollectionResult failed(String source, String error) {
            return new CollectionResult(source, 0, 0, 0, 0, error);
        }
    }

    protected final ActivityService activities;

    protected ActivityCollector(ActivityService activities) {
        this.activities = activities;
    }

    public abstract String source();

    /** Can this collector run without extra parameters (used by "sync everything")? */
    public boolean canAutoRun(User user) {
        return false;
    }

    protected void validate(User user, Map<String, String> params) throws IngestionException {
        // default: nothing to validate
    }

    protected abstract List<CollectedItem> fetch(User user, Map<String, String> params) throws IngestionException;

    public final CollectionResult collect(User user, Map<String, String> params) throws IngestionException {
        long started = System.nanoTime();
        validate(user, params);
        List<CollectedItem> items = fetch(user, params);
        int saved = 0;
        for (CollectedItem item : items) {
            if (activities.ingest(user.getId(), item.request(), source(), item.externalId()).isPresent()) saved++;
        }
        long millis = (System.nanoTime() - started) / 1_000_000;
        return new CollectionResult(source(), items.size(), saved, items.size() - saved, millis, null);
    }
}
