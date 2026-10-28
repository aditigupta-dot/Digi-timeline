package com.digitaltimeline.domain;

import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;

@Entity
@DiscriminatorValue("GITHUB")
public class GitHubActivity extends Activity {

    private String repo;
    private String eventType;
    private String sha;
    private String url;

    @Override
    public ActivityType getType() { return ActivityType.GITHUB; }

    @Override
    public String describe() { return "GitHub " + eventType + " in " + repo; }

    public String getRepo() { return repo; }
    public void setRepo(String repo) { this.repo = repo; }
    public String getEventType() { return eventType; }
    public void setEventType(String eventType) { this.eventType = eventType; }
    public String getSha() { return sha; }
    public void setSha(String sha) { this.sha = sha; }
    public String getUrl() { return url; }
    public void setUrl(String url) { this.url = url; }
}
