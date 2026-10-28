package com.digitaltimeline.collector;

import com.digitaltimeline.collector.ActivityCollector.CollectionResult;
import com.digitaltimeline.domain.User;
import com.digitaltimeline.exception.IngestionException;
import com.digitaltimeline.exception.InvalidRequestException;
import com.digitaltimeline.service.AuthService;
import jakarta.annotation.PreDestroy;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;

/** Registry of collectors + parallel "sync everything" using CompletableFuture on virtual threads. */
@Service
public class CollectorService {

    private final Map<String, ActivityCollector> bySource;
    private final AuthService auth;
    private final ExecutorService pool = Executors.newVirtualThreadPerTaskExecutor();

    public CollectorService(List<ActivityCollector> collectors, AuthService auth) {
        this.bySource = collectors.stream().collect(Collectors.toMap(ActivityCollector::source, Function.identity()));
        this.auth = auth;
    }

    public CollectionResult run(UUID userId, String source, Map<String, String> params) throws IngestionException {
        ActivityCollector collector = bySource.get(source);
        if (collector == null) throw new InvalidRequestException("Unknown source: " + source);
        return collector.collect(auth.require(userId), params);
    }

    public List<CollectionResult> syncAll(UUID userId) {
        User user = auth.require(userId);
        List<CompletableFuture<CollectionResult>> futures = bySource.values().stream()
                .filter(c -> c.canAutoRun(user))
                .map(c -> CompletableFuture.supplyAsync(() -> {
                            try {
                                return c.collect(user, Map.of());
                            } catch (IngestionException e) {
                                return CollectionResult.failed(c.source(), e.getMessage());
                            }
                        }, pool)
                        .orTimeout(45, TimeUnit.SECONDS)
                        .exceptionally(ex -> CollectionResult.failed(c.source(), "Timed out or failed: " + ex.getMessage())))
                .toList();
        return futures.stream().map(CompletableFuture::join).toList();
    }

    public List<String> sources() {
        return bySource.keySet().stream().sorted().toList();
    }

    @PreDestroy
    void shutdown() {
        pool.shutdown();
    }
}
