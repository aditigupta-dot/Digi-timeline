package com.digitaltimeline.core;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/** Minimal iCalendar (.ics) reader: handles line folding, VEVENT blocks, UTC / floating / all-day times. */
public final class IcsParser {

    public record IcsEvent(String uid, String summary, String description, String location, Instant start, Instant end) {}

    private static final DateTimeFormatter DATE_TIME = DateTimeFormatter.ofPattern("yyyyMMdd'T'HHmmss");
    private static final DateTimeFormatter DATE_ONLY = DateTimeFormatter.ofPattern("yyyyMMdd");

    private IcsParser() {}

    public static List<IcsEvent> parse(String ics, ZoneId floatingZone) {
        String unfolded = ics.replace("\r\n", "\n").replaceAll("\n[ \t]", "");
        List<IcsEvent> events = new ArrayList<>();
        boolean inEvent = false;
        String uid = null, summary = null, description = null, location = null;
        Instant start = null, end = null;

        for (String line : unfolded.split("\n")) {
            if (line.equals("BEGIN:VEVENT")) {
                inEvent = true;
                uid = summary = description = location = null;
                start = end = null;
            } else if (line.equals("END:VEVENT")) {
                if (inEvent && start != null && summary != null) {
                    String id = uid != null ? uid : summary + "@" + start;
                    events.add(new IcsEvent(id, summary, description, location, start, end));
                }
                inEvent = false;
            } else if (inEvent) {
                int colon = line.indexOf(':');
                if (colon < 0) continue;
                String key = line.substring(0, colon);
                String value = unescape(line.substring(colon + 1));
                String name = key.contains(";") ? key.substring(0, key.indexOf(';')) : key;
                switch (name) {
                    case "UID" -> uid = value;
                    case "SUMMARY" -> summary = value;
                    case "DESCRIPTION" -> description = value;
                    case "LOCATION" -> location = value;
                    case "DTSTART" -> start = parseTime(key, value, floatingZone).orElse(null);
                    case "DTEND" -> end = parseTime(key, value, floatingZone).orElse(null);
                    default -> { }
                }
            }
        }
        return events;
    }

    private static Optional<Instant> parseTime(String key, String value, ZoneId floatingZone) {
        try {
            if (key.contains("VALUE=DATE") && !key.contains("DATE-TIME") || value.length() == 8) {
                return Optional.of(LocalDate.parse(value, DATE_ONLY).atStartOfDay(floatingZone).toInstant());
            }
            if (value.endsWith("Z")) {
                return Optional.of(LocalDateTime.parse(value.substring(0, value.length() - 1), DATE_TIME)
                        .toInstant(ZoneOffset.UTC));
            }
            ZoneId zone = floatingZone;
            int tz = key.indexOf("TZID=");
            if (tz >= 0) {
                String id = key.substring(tz + 5);
                if (id.contains(";")) id = id.substring(0, id.indexOf(';'));
                try { zone = ZoneId.of(id); } catch (Exception ignored) { /* keep floating zone */ }
            }
            return Optional.of(LocalDateTime.parse(value, DATE_TIME).atZone(zone).toInstant());
        } catch (RuntimeException e) {
            return Optional.empty();
        }
    }

    private static String unescape(String s) {
        return s.replace("\\n", "\n").replace("\\N", "\n").replace("\\,", ",").replace("\\;", ";").replace("\\\\", "\\");
    }
}
