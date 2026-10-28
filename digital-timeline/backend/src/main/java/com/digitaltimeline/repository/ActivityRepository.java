package com.digitaltimeline.repository;

import com.digitaltimeline.domain.Activity;
import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ActivityRepository extends JpaRepository<Activity, UUID> {

    List<Activity> findByUserIdAndOccurredAtBetweenOrderByOccurredAtDesc(UUID userId, Instant from, Instant to);

    List<Activity> findByUserId(UUID userId);

    Optional<Activity> findByIdAndUserId(UUID id, UUID userId);

    List<Activity> findByIdInAndUserId(Collection<UUID> ids, UUID userId);

    boolean existsByUserIdAndSourceAndExternalId(UUID userId, String source, String externalId);

    Optional<Activity> findFirstByUserIdAndProjectAndOccurredAtBeforeAndIdNotOrderByOccurredAtDesc(
            UUID userId, String project, Instant before, UUID excludeId);

    long countByUserId(UUID userId);
}
