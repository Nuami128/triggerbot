package com.example.triggerbot;

import com.example.triggerbot.module.ModuleManager;
import com.example.triggerbot.module.impl.AutoStunModule;

import net.fabricmc.api.ClientModInitializer;

import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;

public class TriggerBotMod implements ClientModInitializer {

    private static final ModuleManager MODULE_MANAGER = ModuleManager.getInstance();

    private static AutoStunModule AUTO_STUN;

    // prevents spam toggling while holding R
    private boolean wasHolding = false;

    @Override
    public void onInitializeClient() {

        System.out.println("TRIGGERBOT INIT");

        // Create module
        AUTO_STUN = new AutoStunModule();

        // Register module
        MODULE_MANAGER.register(AUTO_STUN);

        // Register keybind
        KeyBindingHelper.registerKeyBinding(AutoStunModule.KEYBIND);

        // Main tick loop
        ClientTickEvents.END_CLIENT_TICK.register(client -> {

            boolean holding = AutoStunModule.KEYBIND.isPressed();

            // Detect fresh press
            if (holding && !wasHolding) {

                if (!AUTO_STUN.isEnabled()) {
                    AUTO_STUN.onEnable();
                } else {
                    AUTO_STUN.onDisable();
                }
            }

            wasHolding = holding;

            MODULE_MANAGER.tickAll();
        });
    }

    public static ModuleManager getModuleManager() {
        return MODULE_MANAGER;
    }
}
