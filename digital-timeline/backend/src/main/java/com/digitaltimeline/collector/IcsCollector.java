package com.digitaltimeline.collector;

import com.digitaltimeline.core.IcsParser;
import com.digitaltimeline.domain.ActivityType;
import com.digitaltimeline.domain.User;
import com.digitaltimeline.dto.ActivityRequest;
import com.digitaltimeline.exception.IngestionException;
import com.digitaltimeline.service.ActivityService;
import java.time.DateTimeException;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.springframework.stereotype.Component;

/** Imports a calendar export (.ics from Google Calendar, Outlook, Apple...) as EVENT activities. */
@Component
public class IcsCollector extends ActivityCollector {

    public IcsCollector(ActivityService activities) {
        super(activities);
    }

    @Override
    public String source() {
        return "calendar";
    }

    @Override
    protected void validate(User user, Map<String, String> params) throws IngestionException {
        String content = params.get("content");
        if (content == null || !content.contains("BEGIN:VCALENDAR")) {
            throw new IngestionException(source(), "That file does not look like an .ics calendar");
        }
    }

    @Override
    protected List<CollectedItem> fetch(User user, Map<String, String> params) {
        ZoneId zone;
        try {
            zone = params.get("tz") == null ? ZoneOffset.UTC : ZoneId.of(params.get("tz"));
        } catch (DateTimeException e) {
            zone = ZoneOffset.UTC;
        }
        return IcsParser.parse(params.get("content"), zone).stream().map(e -> {
            Map<String, String> attrs = new HashMap<>();
            if (e.location() != null) attrs.put("location", e.location());
            if (e.end() != null) attrs.put("endsAt", e.end().toString());
            return new CollectedItem(e.uid() + "@" + e.start(),
                    new ActivityRequest(ActivityType.EVENT, e.summary(), e.description(), e.start(), null,
                            Set.of("calendar"), attrs));
        }).toList();
    }
}
