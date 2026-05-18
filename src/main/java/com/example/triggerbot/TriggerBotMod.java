package com.example.triggerbot;

import com.example.triggerbot.module.ModuleManager;
import com.example.triggerbot.module.impl.AutoStunModule;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;

public class TriggerBotMod implements ClientModInitializer {
    private static final ModuleManager MODULE_MANAGER = ModuleManager.getInstance();

    @Override
    public void onInitializeClient() {
        // Register your modules here
        MODULE_MANAGER.register(new AutoStunModule());

        // Tick hook
        ClientTickEvents.END_CLIENT_TICK.register(client -> MODULE_MANAGER.tickAll());

        // Example toggle: enable module directly
        MODULE_MANAGER.enable("AutoStun");
    }

    public static ModuleManager getModuleManager() {
        return MODULE_MANAGER;
    }
}
