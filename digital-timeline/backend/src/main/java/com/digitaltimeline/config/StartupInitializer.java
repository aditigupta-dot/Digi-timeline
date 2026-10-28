package com.digitaltimeline.config;

import com.digitaltimeline.domain.Activity;
import com.digitaltimeline.graph.GraphService;
import com.digitaltimeline.link.LinkingService;
import com.digitaltimeline.repository.ActivityRepository;
import com.digitaltimeline.seed.DemoDataSeeder;
import com.digitaltimeline.service.SearchIndex;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/** On startup: warm the search index from the DB, rebuild an in-memory graph, then optionally seed demo data. */
@Component
public class StartupInitializer {
    private static final Logger log = LoggerFactory.getLogger(StartupInitializer.class);

    private final ActivityRepository repository;
    private final SearchIndex index;
    private final GraphService graph;
    private final LinkingService linking;
    private final DemoDataSeeder seeder;
    private final boolean seedDemo;

    public StartupInitializer(ActivityRepository repository, SearchIndex index, GraphService graph,
                              LinkingService linking, DemoDataSeeder seeder,
                              @Value("${dt.demo.seed:true}") boolean seedDemo) {
        this.repository = repository;
        this.index = index;
        this.graph = graph;
        this.linking = linking;
        this.seeder = seeder;
        this.seedDemo = seedDemo;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void onReady() {
        List<Activity> all = repository.findAll();
        all.forEach(index::put);
        log.info("Search index warmed with {} activities", all.size());
        if ("memory".equals(graph.backend()) && !all.isEmpty()) {
            all.forEach(a -> linking.link(a.getId()));
            log.info("In-memory knowledge graph rebuilt");
        }
        if (seedDemo) seeder.seedIfEmpty();
    }
}
