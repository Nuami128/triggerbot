package com.example.triggerbot.module;

import com.example.triggerbot.module.impl.AutoStunModule; // import your module
import java.util.List; // import List

public class ModuleManager {

    private final List<ClientModule> modules = List.of(
        new AutoStunModule()
        // add other modules here
    );

    public List<ClientModule> getModules() {
        return modules;
    }

    // Initialize all modules
    public void initialize() {
        modules.forEach(ClientModule::onEnable);
    }

    public void tickAll() {
        for (ClientModule module : modules) {
            module.onTick();
        }
    }
}
