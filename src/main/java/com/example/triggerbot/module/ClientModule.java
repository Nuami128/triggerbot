package com.example.triggerbot.module;

public interface ClientModule {

    String getName();

    void onEnable();

    void onDisable();

    void onTick();

    boolean isEnabled();
}
