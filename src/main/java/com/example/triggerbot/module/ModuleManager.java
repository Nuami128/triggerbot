package com.example.triggerbot.module;

import java.util.ArrayList;
import java.util.List;

public class ModuleManager {

    private static final ModuleManager INSTANCE = new ModuleManager();

    public static ModuleManager getInstance() {
        return INSTANCE;
    }

    private final List<ClientModule> modules = new ArrayList<>();

    // -----------------------------
    // REGISTER MODULES
    // -----------------------------
    public void register(ClientModule module) {
        modules.add(module);
    }

    // -----------------------------
    // MAIN TICK LOOP
    // -----------------------------
    public void tickAll() {
        for (ClientModule module : modules) {

            if (module == null) continue;

            module.onTick();
        }
    }

    // -----------------------------
    // OPTIONAL HELPERS
    // -----------------------------
    public void enableAll() {
        for (ClientModule module : modules) {
            module.onEnable();
        }
    }

    public void disableAll() {
        for (ClientModule module : modules) {
            module.onDisable();
        }
    }

    public List<ClientModule> getModules() {
        return modules;
    }
}
