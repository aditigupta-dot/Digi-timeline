package com.digitaltimeline.ingest;

import com.digitaltimeline.domain.Activity;

/** One link in the CHAIN OF RESPONSIBILITY that every new activity travels through before it is saved. */
public interface IngestionStage {
    String name();

    void process(Activity activity);
}
