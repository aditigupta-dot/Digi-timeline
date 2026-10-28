package com.digitaltimeline.domain;

import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import java.time.Instant;

@Entity
@DiscriminatorValue("TASK")
public class TaskActivity extends Activity {

    @Enumerated(EnumType.STRING)
    private TaskStatus status = TaskStatus.TODO;

    private Integer priority = 2;          // 1 = high, 2 = normal, 3 = low

    private Instant dueAt;

    @Override
    public ActivityType getType() { return ActivityType.TASK; }

    @Override
    public String describe() {
        return "Task [" + status + ", priority " + priority + (dueAt != null ? ", due " + dueAt : "") + "]";
    }

    public boolean isOverdue(Instant now) {
        return status != null && status.isOpen() && dueAt != null && dueAt.isBefore(now);
    }

    public TaskStatus getStatus() { return status; }
    public void setStatus(TaskStatus status) { this.status = status; }
    public Integer getPriority() { return priority; }
    public void setPriority(Integer priority) { this.priority = priority; }
    public Instant getDueAt() { return dueAt; }
    public void setDueAt(Instant dueAt) { this.dueAt = dueAt; }
}
