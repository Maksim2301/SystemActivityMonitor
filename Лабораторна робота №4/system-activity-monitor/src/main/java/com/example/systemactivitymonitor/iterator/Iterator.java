package com.example.systemactivitymonitor.iterator;

public interface Iterator<T> {
    void first();            // перейти на перший елемент
    void next();             // перейти на наступний елемент
    boolean isDone();        // чи закінчився обхід
    T currentItem();         // поточний елемент
}

