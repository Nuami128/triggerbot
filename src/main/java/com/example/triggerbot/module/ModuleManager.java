package com.example.triggerbot.module;

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
        modules.forEach(ClientModule::onEnable); // or just prepare modules
    }

    public void tickAll() {
        for (ClientModule module : modules) {
            module.onTick();
        }
    }
}
