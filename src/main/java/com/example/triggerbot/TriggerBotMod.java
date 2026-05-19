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

    System.out.println("MOD INIT RUNNING");

    ClientTickEvents.END_CLIENT_TICK.register(client -> {
        System.out.println("CLIENT TICK RUNNING");
    });
}

    public static ModuleManager getModuleManager() {
        return MODULE_MANAGER;
    }
}
