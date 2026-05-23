package com.example.triggerbot;

import com.example.triggerbot.module.ModuleManager;
import com.example.triggerbot.module.impl.AutoStunModule;
import com.example.triggerbot.module.impl.TriggerBotModule;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;

public class TriggerBotMod implements ClientModInitializer {

    private static final ModuleManager MODULE_MANAGER = ModuleManager.getInstance();
    private static AutoStunModule AUTO_STUN;
    private static TriggerBotModule TRIGGER;

    @Override
    public void onInitializeClient() {
        System.out.println("TRIGGERBOT INIT");

        AUTO_STUN = new AutoStunModule();
        TRIGGER = new TriggerBotModule();

        MODULE_MANAGER.register(AUTO_STUN);
        MODULE_MANAGER.register(TRIGGER);

        // TriggerBot always on
        TRIGGER.onEnable();

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (client.player == null) return;

            // AutoStun fires once per fresh left click press
            if (client.options.attackKey.wasPressed()) {
                AUTO_STUN.onEnable();
            }

            MODULE_MANAGER.tickAll();
        });
    }

    public static ModuleManager getModuleManager() {
        return MODULE_MANAGER;
    }
}
