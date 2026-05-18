package com.example.triggerbot;

import com.example.triggerbot.module.ModuleManager;
import com.example.triggerbot.module.impl.AutoStunModule;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;

public class TriggerBotMod implements ClientModInitializer {

    private static final ModuleManager MODULE_MANAGER = ModuleManager.getInstance();

    @Override
    public void onInitializeClient() {

        System.out.println("TRIGGERBOT INIT LOADED");

        // Register module
        MODULE_MANAGER.register(new AutoStunModule());

        // Register keybind
        KeyBindingHelper.registerKeyBinding(AutoStunModule.KEYBIND);

        // Main tick loop
        ClientTickEvents.END_CLIENT_TICK.register(client -> {

            if (AutoStunModule.KEYBIND.wasPressed()) {

                System.out.println("R PRESSED");

                AutoStunModule autoStun =
                        (AutoStunModule) MODULE_MANAGER
                                .find("AutoStun")
                                .orElse(null);

                if (autoStun != null) {

                    if (!autoStun.isEnabled()) {
                        autoStun.onEnable();
                    } else {
                        autoStun.trigger();
                    }
                }
            }

            MODULE_MANAGER.tickAll();
        });
    }

    public static ModuleManager getModuleManager() {
        return MODULE_MANAGER;
    }
}
