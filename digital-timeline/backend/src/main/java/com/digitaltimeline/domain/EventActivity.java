package com.digitaltimeline.domain;

import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;
import java.time.Instant;

@Entity
@DiscriminatorValue("EVENT")
public class EventActivity extends Activity {

    private Instant endsAt;
    private String location;
    private String attendees;

    @Override
    public ActivityType getType() { return ActivityType.EVENT; }

    @Override
    public String describe() {
        return "Event" + (location != null && !location.isBlank() ? " at " + location : "");
    }

    public Instant getEndsAt() { return endsAt; }
    public void setEndsAt(Instant endsAt) { this.endsAt = endsAt; }
    public String getLocation() { return location; }
    public void setLocation(String location) { this.location = location; }
    public String getAttendees() { return attendees; }
    public void setAttendees(String attendees) { this.attendees = attendees; }
}
