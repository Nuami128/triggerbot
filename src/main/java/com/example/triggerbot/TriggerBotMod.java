package com.example.triggerbot;

import com.example.triggerbot.module.ModuleManager;
import com.example.triggerbot.module.impl.AutoStunModule;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;

public class TriggerBotMod implements ClientModInitializer {

    private static final ModuleManager MODULE_MANAGER = ModuleManager.getInstance();
    private static AutoStunModule AUTO_STUN;

    @Override
    public void onInitializeClient() {
        System.out.println("TRIGGERBOT INIT");

        AUTO_STUN = new AutoStunModule();
        MODULE_MANAGER.register(AUTO_STUN);

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (client.player == null) return;

            boolean attacking = client.options.attackKey.isPressed();

            if (attacking) {
                if (!AUTO_STUN.isEnabled()) {
                    AUTO_STUN.onEnable();
                }
            } else {
                if (AUTO_STUN.isEnabled()) {
                    AUTO_STUN.beginSwapBack();
                }
            }

            MODULE_MANAGER.tickAll();
        });
    }

    public static ModuleManager getModuleManager() {
        return MODULE_MANAGER;
    }
}
