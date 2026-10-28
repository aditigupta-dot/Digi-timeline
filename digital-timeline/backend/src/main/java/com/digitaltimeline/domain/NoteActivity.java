package com.digitaltimeline.domain;

import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;

@Entity
@DiscriminatorValue("NOTE")
public class NoteActivity extends Activity {

    private String noteFormat = "markdown";

    @Override
    public ActivityType getType() { return ActivityType.NOTE; }

    @Override
    public String describe() { return "Note (" + noteFormat + ")"; }

    public String getNoteFormat() { return noteFormat; }
    public void setNoteFormat(String noteFormat) { this.noteFormat = noteFormat; }
}
