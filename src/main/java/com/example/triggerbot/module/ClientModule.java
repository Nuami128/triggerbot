package com.example.triggerbot;

import com.example.triggerbot.module.ModuleManager;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.MinecraftClient;

public class ClientModule implements ClientModInitializer {

    @Override
    public void onInitializeClient() {

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (client.player == null || client.world == null) return;

            ModuleManager.getInstance().postMovementAll();
        });

        System.out.println("Client mod initialized");
    }
}
