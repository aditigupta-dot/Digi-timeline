package com.digitaltimeline.config;

import com.digitaltimeline.graph.GraphStore;
import com.digitaltimeline.graph.InMemoryGraphStore;
import com.digitaltimeline.graph.Neo4jGraphStore;
import org.neo4j.driver.Driver;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class GraphConfig {

    @Bean
    GraphStore graphStore(@Value("${dt.graph.store:memory}") String store, org.springframework.beans.factory.ObjectProvider<Driver> driver) {
        if ("neo4j".equalsIgnoreCase(store) && driver.getIfAvailable() != null) {
            return new Neo4jGraphStore(driver.getIfAvailable());
        }
        return new InMemoryGraphStore();
    }
}
