package com.digitaltimeline.web;

import com.digitaltimeline.collector.ActivityCollector.CollectionResult;
import com.digitaltimeline.collector.CollectorService;
import com.digitaltimeline.dto.ActivityResponse;
import com.digitaltimeline.exception.IngestionException;
import com.digitaltimeline.exception.InvalidRequestException;
import com.digitaltimeline.export.SnapshotService;
import com.digitaltimeline.service.ActivityService;
import com.digitaltimeline.service.AuthService;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@RestController
@RequestMapping("/api")
public class DataController {

    private final CollectorService collectors;
    private final SnapshotService snapshots;
    private final ActivityService activities;
    private final AuthService auth;
    private final ObjectMapper mapper;
    private final TimelineStreamService stream;

    public DataController(CollectorService collectors, SnapshotService snapshots, ActivityService activities,
                          AuthService auth, ObjectMapper mapper, TimelineStreamService stream) {
        this.collectors = collectors;
        this.snapshots = snapshots;
        this.activities = activities;
        this.auth = auth;
        this.mapper = mapper;
        this.stream = stream;
    }

    @PostMapping("/collect/sync-all")
    public List<CollectionResult> syncAll(@AuthenticationPrincipal Jwt jwt) {
        return collectors.syncAll(CurrentUser.id(jwt));
    }

    @PostMapping("/collect/github")
    public CollectionResult github(@AuthenticationPrincipal Jwt jwt, @RequestBody(required = false) Map<String, String> body)
            throws IngestionException {
        return collectors.run(CurrentUser.id(jwt), "github", body == null ? Map.of() : body);
    }

    @PostMapping("/collect/folder")
    public CollectionResult folder(@AuthenticationPrincipal Jwt jwt, @RequestBody Map<String, String> body)
            throws IngestionException {
        return collectors.run(CurrentUser.id(jwt), "folder", body);
    }

    @PostMapping("/collect/calendar")
    public CollectionResult calendar(@AuthenticationPrincipal Jwt jwt, @RequestParam("file") MultipartFile file,
                                     @RequestParam(required = false) String tz) throws IngestionException, IOException {
        if (file.getSize() > 5_000_000) throw new InvalidRequestException("Calendar file is larger than 5 MB");
        Map<String, String> params = new HashMap<>();
        params.put("content", new String(file.getBytes(), StandardCharsets.UTF_8));
        if (tz != null) params.put("tz", tz);
        return collectors.run(CurrentUser.id(jwt), "calendar", params);
    }

    @GetMapping("/export/snapshot")
    public ResponseEntity<byte[]> snapshot(@AuthenticationPrincipal Jwt jwt) throws IOException {
        UUID id = CurrentUser.id(jwt);
        byte[] bytes = snapshots.export(id, auth.require(id).getDisplayName());
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment().filename("timeline.dtsnap").build().toString())
                .contentType(MediaType.APPLICATION_OCTET_STREAM).body(bytes);
    }

    @GetMapping("/export/json")
    public ResponseEntity<byte[]> json(@AuthenticationPrincipal Jwt jwt) throws IOException {
        List<ActivityResponse> all = activities.list(CurrentUser.id(jwt), Instant.EPOCH, Instant.now().plusSeconds(86400), null, null);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment().filename("timeline.json").build().toString())
                .contentType(MediaType.APPLICATION_JSON).body(mapper.writerWithDefaultPrettyPrinter().writeValueAsBytes(all));
    }

    @PostMapping("/import/snapshot")
    public SnapshotService.ImportResult restore(@AuthenticationPrincipal Jwt jwt, @RequestParam("file") MultipartFile file)
            throws IOException {
        return snapshots.restore(CurrentUser.id(jwt), file.getInputStream());
    }

    /** Live updates. The browser connects with ?access_token=... because EventSource cannot set headers. */
    @GetMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter stream(@AuthenticationPrincipal Jwt jwt) {
        return stream.subscribe(CurrentUser.id(jwt));
    }
}
