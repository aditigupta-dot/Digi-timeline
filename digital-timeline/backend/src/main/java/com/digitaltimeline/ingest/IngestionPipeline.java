package com.digitaltimeline.ingest;

import com.digitaltimeline.domain.Activity;
import java.util.List;
import org.springframework.stereotype.Component;

/** Runs every IngestionStage in @Order. Add a new stage = add a new @Component, no other code changes (open/closed). */
@Component
public class IngestionPipeline {

    private final List<IngestionStage> stages;

    public IngestionPipeline(List<IngestionStage> stages) {
        this.stages = List.copyOf(stages);
    }

    public Activity run(Activity activity) {
        stages.forEach(stage -> stage.process(activity));
        return activity;
    }

    public List<String> stageNames() {
        return stages.stream().map(IngestionStage::name).toList();
    }
}
