package com.digitaltimeline.ingest;

import com.digitaltimeline.domain.Activity;
import com.digitaltimeline.domain.DocumentActivity;
import com.digitaltimeline.domain.EventActivity;
import com.digitaltimeline.domain.GitHubActivity;
import com.digitaltimeline.domain.TaskActivity;
import com.digitaltimeline.domain.TaskStatus;

/** Heuristic 0..1 importance used to size nodes in the graph and rank results. */
public final class ImportanceScorer {
    private ImportanceScorer() {}

    public static double score(Activity a) {
        double s = 0.3;
        if (a instanceof TaskActivity t) {
            int priority = t.getPriority() == null ? 2 : t.getPriority();
            s += switch (priority) {
                case 1 -> 0.4;
                case 2 -> 0.2;
                default -> 0.05;
            };
            if (t.getStatus() == TaskStatus.DONE) s -= 0.1;
        } else if (a instanceof EventActivity e) {
            int people = e.getAttendees() == null || e.getAttendees().isBlank() ? 0 : e.getAttendees().split(",").length;
            s += 0.25 + Math.min(0.2, people * 0.04);
        } else if (a instanceof GitHubActivity) {
            s += 0.15;
        } else if (a instanceof DocumentActivity) {
            s += 0.2;
        } else {
            int len = a.getContent() == null ? 0 : a.getContent().length();
            s += 0.1 + Math.min(0.2, len / 2000.0);
        }
        return Math.max(0, Math.min(1, s));
    }
}
