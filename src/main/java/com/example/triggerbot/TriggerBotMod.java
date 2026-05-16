package com.example.triggerbot;

import com.example.triggerbot.module.ModuleManager;
import net.fabricmc.api.ClientModInitializer;

public class TriggerBotMod implements ClientModInitializer {
    private static final ModuleManager MODULE_MANAGER = new ModuleManager();

    @Override
    public void onInitializeClient() {
        MODULE_MANAGER.initialize();
    }

    public static ModuleManager getModuleManager() {
        return MODULE_MANAGER;
    }
}
