package com.example.systemactivitymonitor.iterator;

public interface Iterator<T> {
    void first();
    void next();
    boolean isDone();
    T currentItem();
}

