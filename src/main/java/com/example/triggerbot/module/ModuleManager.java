// Minecraft 1.21.11 (Fabric)
package com.example.modules.api;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

/**
 * Central registry that owns every {@link ClientModule}.
 *
 * Usage (e.g. in your Fabric mod initialiser):
 * <pre>
 *   ModuleManager.getInstance().register(new AutoStunModule());
 * </pre>
 *
 * Your Fabric tick event handler should call:
 * <pre>
 *   ModuleManager.getInstance().tickAll();
 * </pre>
 */
public final class ModuleManager {

    // ── Singleton ──────────────────────────────────────────────────────────────
    private static final ModuleManager INSTANCE = new ModuleManager();

    public static ModuleManager getInstance() {
        return INSTANCE;
    }

    private ModuleManager() {}

    // ── State ──────────────────────────────────────────────────────────────────
    private final List<ClientModule> modules = new ArrayList<>();

    // ── Registration ──────────────────────────────────────────────────────────

    /**
     * Registers a module. Safe to call multiple times with the same module
     * (duplicate names are rejected).
     */
    public void register(ClientModule module) {
        boolean duplicate = modules.stream()
                .anyMatch(m -> m.getName().equalsIgnoreCase(module.getName()));
        if (duplicate) {
            throw new IllegalArgumentException(
                    "A module named '" + module.getName() + "' is already registered.");
        }
        modules.add(module);
    }

    /** Removes a previously registered module by name (case-insensitive). */
    public void unregister(String name) {
        modules.removeIf(m -> m.getName().equalsIgnoreCase(name));
    }

    // ── Lifecycle ──────────────────────────────────────────────────────────────

    /**
     * Calls {@link ClientModule#onTick()} on every registered module.
     * Hook this into your Fabric {@code ClientTickEvents.END_CLIENT_TICK} listener.
     */
    public void tickAll() {
        for (ClientModule module : modules) {
            module.onTick();
        }
    }

    /** Enables a module by name, calling its {@link ClientModule#onEnable()}. */
    public void enable(String name) {
        find(name).ifPresent(ClientModule::onEnable);
    }

    /** Disables a module by name, calling its {@link ClientModule#onDisable()}. */
    public void disable(String name) {
        find(name).ifPresent(ClientModule::onDisable);
    }

    // ── Queries ────────────────────────────────────────────────────────────────

    /** Returns an unmodifiable view of all registered modules. */
    public List<ClientModule> getAll() {
        return Collections.unmodifiableList(modules);
    }

    /** Finds a module by name (case-insensitive). */
    public Optional<ClientModule> find(String name) {
        return modules.stream()
                .filter(m -> m.getName().equalsIgnoreCase(name))
                .findFirst();
    }
}
