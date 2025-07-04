package com.project.syncly.domain.folder.entity;

public enum FolderDepth {
    MAX(5);
    private final int value;
    FolderDepth(int value) { this.value = value; }
    public int getValue() { return value; }
}
