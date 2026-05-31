package com.example.triggerbot.module;

public interface ClientModule {
    String getName();
    boolean isEnabled();
    void onEnable();
    void onDisable();
    void onTick();
    void onPostMovement();
    void onJumpReset();
    void onDamage(); // ADD THIS
}
