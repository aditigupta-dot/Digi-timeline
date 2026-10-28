package com.digitaltimeline.export;

import com.digitaltimeline.domain.Activity;
import com.digitaltimeline.domain.ActivityType;
import com.digitaltimeline.dto.ActivityRequest;
import com.digitaltimeline.dto.ActivityResponse;
import com.digitaltimeline.exception.InvalidRequestException;
import com.digitaltimeline.repository.ActivityRepository;
import com.digitaltimeline.service.ActivityService;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputFilter;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.UUID;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;
import org.springframework.stereotype.Service;

/**
 * Backup and restore with Java serialization (gzip-compressed). Deserialisation is the classic attack surface,
 * so the stream is protected by an ObjectInputFilter allow-list and size limits.
 */
@Service
public class SnapshotService {

    public record ImportResult(int items, int imported, int skipped) {}

    private static final ObjectInputFilter FILTER = ObjectInputFilter.Config.createFilter(
            "maxdepth=12;maxrefs=200000;maxbytes=52428800;maxarray=1000000;"
                    + "com.digitaltimeline.export.*;java.util.*;java.time.*;java.lang.*;!*");

    private final ActivityRepository repository;
    private final ActivityService activities;
    private final Clock clock;

    public SnapshotService(ActivityRepository repository, ActivityService activities, Clock clock) {
        this.repository = repository;
        this.activities = activities;
        this.clock = clock;
    }

    public byte[] export(UUID userId, String ownerName) throws IOException {
        ArrayList<SnapshotItem> items = new ArrayList<>();
        for (Activity a : repository.findByUserId(userId)) {
            ActivityResponse r = ActivityResponse.from(a);
            HashMap<String, String> attrs = new HashMap<>();
            r.details().forEach((k, v) -> { if (v != null) attrs.put(k, String.valueOf(v)); });
            items.add(new SnapshotItem(a.getType().name(), a.getTitle(), a.getContent(), a.getProject(),
                    a.getOccurredAt(), new LinkedHashSet<>(a.getTags()), attrs, a.getEmbedding()));
        }
        TimelineSnapshot snapshot = new TimelineSnapshot(clock.instant(), ownerName, items);
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        try (ObjectOutputStream out = new ObjectOutputStream(new GZIPOutputStream(bytes))) {
            out.writeObject(snapshot);
        }
        return bytes.toByteArray();
    }

    public ImportResult restore(UUID userId, InputStream upload) throws IOException {
        TimelineSnapshot snapshot;
        try (ObjectInputStream in = new ObjectInputStream(new GZIPInputStream(upload))) {
            in.setObjectInputFilter(FILTER);
            Object o = in.readObject();
            if (!(o instanceof TimelineSnapshot s)) throw new InvalidRequestException("Not a DigitalTimeline snapshot");
            snapshot = s;
        } catch (ClassNotFoundException | java.io.ObjectStreamException | java.util.zip.ZipException e) {
            throw new InvalidRequestException("Snapshot is corrupt or from an incompatible version");
        }
        int imported = 0;
        for (SnapshotItem item : snapshot.items()) {
            ActivityRequest request = new ActivityRequest(ActivityType.valueOf(item.type()), item.title(), item.content(),
                    item.occurredAt(), item.project(), item.tags(), item.attrs());
            String key = item.type() + "|" + item.title() + "|" + item.occurredAt();
            String externalId = UUID.nameUUIDFromBytes(key.getBytes(StandardCharsets.UTF_8)).toString();
            if (activities.ingest(userId, request, "snapshot", externalId).isPresent()) imported++;
        }
        return new ImportResult(snapshot.items().size(), imported, snapshot.items().size() - imported);
    }
}
