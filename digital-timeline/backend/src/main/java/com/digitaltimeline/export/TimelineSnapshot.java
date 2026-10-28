package com.digitaltimeline.export;

import java.io.Serializable;
import java.time.Instant;
import java.util.ArrayList;

public class TimelineSnapshot implements Serializable {
    private static final long serialVersionUID = 1L;

    private final int formatVersion = 1;
    private final Instant exportedAt;
    private final String owner;
    private final ArrayList<SnapshotItem> items;

    public TimelineSnapshot(Instant exportedAt, String owner, ArrayList<SnapshotItem> items) {
        this.exportedAt = exportedAt;
        this.owner = owner;
        this.items = items;
    }

    public int formatVersion() { return formatVersion; }
    public Instant exportedAt() { return exportedAt; }
    public String owner() { return owner; }
    public ArrayList<SnapshotItem> items() { return items; }
}
