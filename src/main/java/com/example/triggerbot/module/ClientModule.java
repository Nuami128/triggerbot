// Minecraft 1.21.11 (Fabric)
package com.example.triggerbot.module;

/**
 * Minimal contract every module must satisfy.
 * Add further lifecycle hooks here (onRender, onPacket, etc.)
 * as the framework grows.
 */
public interface ClientModule {

    /** Short, human-readable name shown in the module list / HUD. */
    String getName();

    /** Called once when the module is switched on. */
    void onEnable();

    /** Called once when the module is switched off. */
    void onDisable();

    /**
     * Called every client tick while the module is registered.
     * Implementations are responsible for checking their own enabled state.
     */
    void onTick();
}
