package com.example.triggerbot.module;

import java.util.ArrayList;
import java.util.List;

public class ModuleManager {

    private static final ModuleManager INSTANCE = new ModuleManager();

    public static ModuleManager getInstance() {
        return INSTANCE;
    }

    private final List<ClientModule> modules = new ArrayList<>();

    public void register(ClientModule module) {
        modules.add(module);
    }

    public void postMovementAll() {
        for (ClientModule module : modules) {
            module.onTick();
        }
    }
}
