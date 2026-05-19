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

        System.out.println("TRIGGERBOT LOADED");

        AutoStunModule autoStun = new AutoStunModule();

        MODULE_MANAGER.register(autoStun);

        // REGISTER THE SAME KEYBIND INSTANCE
        KeyBindingHelper.registerKeyBinding(AutoStunModule.KEYBIND);

        ClientTickEvents.END_CLIENT_TICK.register(client -> {

            while (AutoStunModule.KEYBIND.wasPressed()) {

                System.out.println("R PRESSED");

                if (!autoStun.isEnabled()) {
                    autoStun.onEnable();
                } else {
                    autoStun.trigger();
                }
            }

            MODULE_MANAGER.tickAll();
        });
    }

    public static ModuleManager getModuleManager() {
        return MODULE_MANAGER;
    }
}
