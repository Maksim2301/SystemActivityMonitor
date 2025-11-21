package com.example.systemactivitymonitor.iterator;

public interface Aggregate<T> {
    Iterator<T> createIterator();
    int size();
    T get(int index);
}
