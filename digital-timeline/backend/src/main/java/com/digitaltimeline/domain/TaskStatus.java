package com.digitaltimeline.domain;

public enum TaskStatus {
    TODO, IN_PROGRESS, DONE;

    public boolean isOpen() {
        return this != DONE;
    }
}
