package com.example.moneygestor;

public interface LoadingInterface {
    default void show() {show(true);}
    void show(boolean indeterminate);
    void hide();
    void setMaximus(int maximus);
    default boolean increment() {return increment(1);}
    boolean increment(int value);
    void reset();
}
