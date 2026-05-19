package com.example.triggerbot;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;

public class TriggerBotMod implements ClientModInitializer {

    @Override
    public void onInitializeClient() {

        System.out.println("TRIGGERBOT INIT");

        ClientTickEvents.END_CLIENT_TICK.register(client -> {

            if (client.player != null) {
                client.player.sendMessage(
                        net.minecraft.text.Text.literal("TICK WORKS"),
                        true
                );
            }
        });
    }
}
