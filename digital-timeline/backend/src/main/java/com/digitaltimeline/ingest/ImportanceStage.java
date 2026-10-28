package com.digitaltimeline.ingest;

import com.digitaltimeline.domain.Activity;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

@Component
@Order(30)
public class ImportanceStage implements IngestionStage {
    @Override
    public String name() {
        return "importance";
    }

    @Override
    public void process(Activity a) {
        a.setImportance(ImportanceScorer.score(a));
    }
}
