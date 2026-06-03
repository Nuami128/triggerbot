package com.example.triggerbot;

import com.example.triggerbot.module.ModuleManager;
import com.example.triggerbot.module.impl.AutoJumpResetModule;
import com.example.triggerbot.module.impl.AutoSprintModule;
import com.example.triggerbot.module.impl.AutoStunModule;
import com.example.triggerbot.module.impl.TriggerBotModule;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;

public class TriggerBotMod implements ClientModInitializer {

    private static final ModuleManager MODULE_MANAGER = ModuleManager.getInstance();
    private static AutoStunModule AUTO_STUN;
    private static TriggerBotModule TRIGGER;
    private static AutoSprintModule AUTO_SPRINT;
    private static AutoJumpResetModule AUTO_JUMP_RESET;

    @Override
    public void onInitializeClient() {
        System.out.println("TRIGGERBOT INIT");

        AUTO_STUN = new AutoStunModule();
        AUTO_SPRINT = new AutoSprintModule();
        TRIGGER = new TriggerBotModule(AUTO_STUN, AUTO_SPRINT); 
        AUTO_JUMP_RESET = new AutoJumpResetModule();

        MODULE_MANAGER.register(AUTO_SPRINT);
        MODULE_MANAGER.register(AUTO_STUN);
        MODULE_MANAGER.register(TRIGGER);
        MODULE_MANAGER.register(AUTO_JUMP_RESET);

        TRIGGER.onEnable();
        AUTO_SPRINT.onEnable();
        AUTO_JUMP_RESET.onEnable();

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            MODULE_MANAGER.tickAll();
            MODULE_MANAGER.clientTickAll();
        });
    }

    public static ModuleManager getModuleManager() {
        return MODULE_MANAGER;
    }
}

