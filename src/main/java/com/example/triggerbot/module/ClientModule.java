package com.example.triggerbot.module;

public interface ClientModule {
    String getName();

    default void onInitialize() {
    }
}
