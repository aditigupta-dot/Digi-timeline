package com.digitaltimeline.collector;

import com.digitaltimeline.core.TextTools;
import com.digitaltimeline.domain.ActivityType;
import com.digitaltimeline.domain.User;
import com.digitaltimeline.dto.ActivityRequest;
import com.digitaltimeline.exception.IngestionException;
import com.digitaltimeline.service.ActivityService;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * FILE HANDLING: walks a folder (java.nio.file), reads recently-changed text files and turns them into DOCUMENT
 * activities. Security: only folders inside the configured allow-list can be scanned (no path traversal, no symlinks).
 */
@Component
public class FolderCollector extends ActivityCollector {

    private static final Set<String> TEXT_EXT = Set.of("txt", "md", "java", "py", "js", "ts", "tex", "csv", "json",
            "yml", "yaml", "xml", "html", "sql", "ipynb", "rst");
    private static final int MAX_FILES = 500;
    private static final int MAX_CHARS = 4000;

    private final List<String> allowedRoots;

    public FolderCollector(ActivityService activities, @Value("${dt.collect.allowed-roots:./sample-data}") List<String> allowedRoots) {
        super(activities);
        this.allowedRoots = allowedRoots;
    }

    @Override
    public String source() {
        return "folder";
    }

    private Path resolve(Map<String, String> params) throws IngestionException {
        String raw = params.get("path");
        if (raw == null || raw.isBlank()) throw new IngestionException(source(), "Give a folder path");
        try {
            Path requested = Paths.get(raw.trim()).toRealPath();
            if (!Files.isDirectory(requested)) throw new IngestionException(source(), "Not a folder: " + raw);
            for (String allowed : allowedRoots) {
                try {
                    if (requested.startsWith(Paths.get(allowed.trim()).toRealPath())) return requested;
                } catch (IOException ignored) {
                    // allowed root does not exist on this machine: skip it
                }
            }
            throw new IngestionException(source(), "That folder is outside the allowed locations (dt.collect.allowed-roots)");
        } catch (IOException e) {
            throw new IngestionException(source(), "Folder not found: " + raw, e);
        }
    }

    @Override
    protected void validate(User user, Map<String, String> params) throws IngestionException {
        resolve(params);
    }

    @Override
    protected List<CollectedItem> fetch(User user, Map<String, String> params) throws IngestionException {
        Path root = resolve(params);
        int days = parseDays(params.get("days"));
        Instant since = Instant.now().minus(Duration.ofDays(days));
        List<CollectedItem> items = new ArrayList<>();

        try (Stream<Path> walk = Files.walk(root, 4)) {
            List<Path> files = walk.filter(Files::isRegularFile)
                    .filter(p -> !p.getFileName().toString().startsWith("."))
                    .filter(p -> modified(p).isAfter(since))
                    .limit(MAX_FILES)
                    .toList();
            for (Path file : files) items.add(toItem(root, file));
        } catch (IOException e) {
            throw new IngestionException(source(), "Could not read folder: " + e.getMessage(), e);
        }
        return items;
    }

    private CollectedItem toItem(Path root, Path file) {
        String name = file.getFileName().toString();
        String ext = name.contains(".") ? name.substring(name.lastIndexOf('.') + 1).toLowerCase(Locale.ROOT) : "";
        Instant modified = modified(file);
        String content = null;
        if (TEXT_EXT.contains(ext)) {
            try {
                content = TextTools.truncate(Files.readString(file), MAX_CHARS);
            } catch (IOException | RuntimeException ignored) {
                // binary or non-UTF8 file: keep metadata only
            }
        }
        Map<String, String> attrs = new HashMap<>();
        attrs.put("filePath", file.toString());
        try {
            String mime = Files.probeContentType(file);
            attrs.put("mimeType", mime != null ? mime : "application/" + (ext.isEmpty() ? "octet-stream" : ext));
            attrs.put("sizeBytes", String.valueOf(Files.size(file)));
        } catch (IOException ignored) {
            // metadata is optional
        }
        Path parent = root.relativize(file).getParent();
        String project = parent != null ? parent.getName(0).toString() : root.getFileName().toString();
        Set<String> tags = new LinkedHashSet<>();
        if (!ext.isEmpty()) tags.add(ext);
        return new CollectedItem(file.toAbsolutePath() + "@" + modified.toEpochMilli(),
                new ActivityRequest(ActivityType.DOCUMENT, name, content, modified, project, tags, attrs));
    }

    private static Instant modified(Path p) {
        try {
            return Files.getLastModifiedTime(p).toInstant();
        } catch (IOException e) {
            return Instant.EPOCH;
        }
    }

    private static int parseDays(String v) {
        try {
            return v == null ? 30 : Math.max(1, Math.min(365, Integer.parseInt(v.trim())));
        } catch (NumberFormatException e) {
            return 30;
        }
    }
}
