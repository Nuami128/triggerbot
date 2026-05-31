package com.example.triggerbot.module;

public interface ClientModule extends TickListener {
    String getName();
    boolean isEnabled();
    void onEnable();
    void onDisable();
    void onTick();
    void onPostMovement();
    void onJumpReset();
    void onDamage();

    @Override
    default void onClientTick() {}
}
