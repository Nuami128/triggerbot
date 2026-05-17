package com.example.triggerbot.module;

import com.example.triggerbot.module.ClientModule;
import com.example.triggerbot.module.impl.AutoStunModule;
import java.util.List;

public class ModuleManager {

    private final List<ClientModule> modules = List.of(
        new AutoStunModule()
        // add other modules here
    );

    public List<ClientModule> getModules() {
        return modules;
    }

    // Optional: enable all modules on startup
    public void enableAll() {
        modules.forEach(ClientModule::onEnable);
    }

    // Optional: tick all modules each game tick
    public void tickAll() {
        for (ClientModule module : modules) {
            module.onTick();
        }
    }
}
