package com.example.triggerbot.module;

public interface ClientModule {

    String getName();

    void onEnable();

    void onDisable();

    void onTick();

    void onPostMovement();

    void onJumpReset();

    boolean isEnabled();
}
