package com.digitaltimeline.ingest;

import com.digitaltimeline.core.TextTools;
import com.digitaltimeline.domain.Activity;
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Set;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/** Normalises tags, harvests #hashtags from the text and tops up with keywords when a note has few tags. */
@Component
@Order(10)
public class TagExtractionStage implements IngestionStage {

    @Override
    public String name() {
        return "tags";
    }

    @Override
    public void process(Activity a) {
        Set<String> tags = new LinkedHashSet<>();
        a.getTags().stream().map(t -> t.trim().toLowerCase(Locale.ROOT)).filter(t -> !t.isEmpty()).forEach(tags::add);
        String text = a.getTitle() + " " + (a.getContent() == null ? "" : a.getContent());
        tags.addAll(TextTools.hashtags(text));
        if (tags.size() < 3) {
            TextTools.topKeywords(text, 3 - tags.size()).forEach(tags::add);
        }
        a.setTags(tags);
    }
}
