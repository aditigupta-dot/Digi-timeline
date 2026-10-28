package com.digitaltimeline.web;

import com.digitaltimeline.domain.ActivityType;
import com.digitaltimeline.dto.ActivityRequest;
import com.digitaltimeline.dto.ActivityResponse;
import com.digitaltimeline.dto.GraphDtos.RelatedActivity;
import com.digitaltimeline.dto.TimelineDay;
import com.digitaltimeline.graph.GraphService;
import com.digitaltimeline.repository.ActivityRepository;
import com.digitaltimeline.service.ActivityService;
import jakarta.validation.Valid;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class ActivityController {

    private final ActivityService service;
    private final GraphService graph;
    private final ActivityRepository repository;
    private final Clock clock;

    public ActivityController(ActivityService service, GraphService graph, ActivityRepository repository, Clock clock) {
        this.service = service;
        this.graph = graph;
        this.repository = repository;
        this.clock = clock;
    }

    /** The main timeline: activities grouped by local day, newest day first. */
    @GetMapping("/timeline")
    public List<TimelineDay> timeline(@AuthenticationPrincipal Jwt jwt,
                                      @RequestParam(defaultValue = "30") int days,
                                      @RequestParam(required = false) Set<ActivityType> types,
                                      @RequestParam(required = false) String project,
                                      @RequestParam(required = false) String tz,
                                      @RequestParam(defaultValue = "1") int ahead) {
        ZoneId zone = CurrentUser.zone(tz);
        Instant to = clock.instant().plus(Math.max(1, Math.min(ahead, 365)), ChronoUnit.DAYS);
        Instant from = clock.instant().minus(Math.max(1, Math.min(days, 365)), ChronoUnit.DAYS);
        TreeMap<java.time.LocalDate, List<ActivityResponse>> grouped = new TreeMap<>(Comparator.reverseOrder());
        for (ActivityResponse a : service.list(CurrentUser.id(jwt), from, to, types, project)) {
            grouped.computeIfAbsent(a.occurredAt().atZone(zone).toLocalDate(), d -> new ArrayList<>()).add(a);
        }
        return grouped.entrySet().stream().map(e -> new TimelineDay(e.getKey(), e.getValue())).toList();
    }

    @GetMapping("/projects")
    public List<String> projects(@AuthenticationPrincipal Jwt jwt) {
        return repository.findByUserId(CurrentUser.id(jwt)).stream().map(a -> a.getProject())
                .filter(p -> p != null && !p.isBlank()).distinct().sorted().toList();
    }

    @PostMapping("/activities")
    @ResponseStatus(HttpStatus.CREATED)
    public ActivityResponse create(@AuthenticationPrincipal Jwt jwt, @Valid @RequestBody ActivityRequest r) {
        return service.create(CurrentUser.id(jwt), r);
    }

    @GetMapping("/activities/{id}")
    public ActivityResponse get(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID id) {
        return service.get(CurrentUser.id(jwt), id);
    }

    @PutMapping("/activities/{id}")
    public ActivityResponse update(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID id,
                                   @Valid @RequestBody ActivityRequest r) {
        return service.update(CurrentUser.id(jwt), id, r);
    }

    @DeleteMapping("/activities/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID id) {
        service.delete(CurrentUser.id(jwt), id);
    }

    /** Knowledge-graph neighbours of one activity, each with the reason it is connected. */
    @GetMapping("/activities/{id}/related")
    public List<RelatedActivity> related(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID id) {
        UUID userId = CurrentUser.id(jwt);
        service.getEntity(userId, id);
        var edges = graph.neighbours(id, 8);
        Map<UUID, com.digitaltimeline.domain.Activity> byId = new HashMap<>();
        repository.findByIdInAndUserId(edges.stream().map(e -> e.to()).collect(Collectors.toSet()), userId)
                .forEach(a -> byId.put(a.getId(), a));
        return edges.stream().filter(e -> byId.containsKey(e.to()))
                .map(e -> new RelatedActivity(ActivityResponse.from(byId.get(e.to())), e.kind(), e.weight(), e.reason()))
                .toList();
    }
}
