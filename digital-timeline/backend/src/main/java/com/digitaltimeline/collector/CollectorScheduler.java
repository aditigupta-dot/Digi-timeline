package com.digitaltimeline.collector;

import com.digitaltimeline.domain.User;
import com.digitaltimeline.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/** Background sync (enable with dt.collect.auto-sync=true). */
@Component
@ConditionalOnProperty(name = "dt.collect.auto-sync", havingValue = "true")
public class CollectorScheduler {
    private static final Logger log = LoggerFactory.getLogger(CollectorScheduler.class);

    private final CollectorService collectors;
    private final UserRepository users;

    public CollectorScheduler(CollectorService collectors, UserRepository users) {
        this.collectors = collectors;
        this.users = users;
    }

    @Scheduled(fixedDelayString = "${dt.collect.github-interval:PT30M}", initialDelayString = "PT1M")
    public void sync() {
        for (User u : users.findByGithubUsernameIsNotNull()) {
            collectors.syncAll(u.getId()).forEach(r ->
                    log.info("Auto-sync {} for {}: saved {}, skipped {}{}", r.source(), u.getEmail(), r.saved(),
                            r.skipped(), r.error() != null ? ", error: " + r.error() : ""));
        }
    }
}
