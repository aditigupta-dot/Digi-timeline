package com.digitaltimeline.domain;

import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;

@Entity
@DiscriminatorValue("DOCUMENT")
public class DocumentActivity extends Activity {

    private String filePath;
    private String mimeType;
    private Long sizeBytes;

    @Override
    public ActivityType getType() { return ActivityType.DOCUMENT; }

    @Override
    public String describe() {
        return "Document" + (mimeType != null ? " (" + mimeType + ")" : "")
                + (sizeBytes != null ? ", " + sizeBytes / 1024 + " KB" : "");
    }

    public String getFilePath() { return filePath; }
    public void setFilePath(String filePath) { this.filePath = filePath; }
    public String getMimeType() { return mimeType; }
    public void setMimeType(String mimeType) { this.mimeType = mimeType; }
    public Long getSizeBytes() { return sizeBytes; }
    public void setSizeBytes(Long sizeBytes) { this.sizeBytes = sizeBytes; }
}
