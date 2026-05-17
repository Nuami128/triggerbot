package com.example.triggerbot.module.impl;

import net.minecraft.client.MinecraftClient;
import net.minecraft.text.Text;

public class AutoStunModule {

    private final MinecraftClient client = MinecraftClient.getInstance();
    private boolean enabled = false;

    // Call this to enable the module
    public void enable() {
        enabled = true;
        sendHotbarMessage("Auto Stun Enabled");
    }

    // Call this to disable the module
    public void disable() {
        enabled = false;
        sendHotbarMessage("Auto Stun Disabled");
    }

    // Call this every tick from your main tick handler
    public void tick() {
        if (!enabled) return;

        // Debug tick message
        System.out.println("AutoStunModule tick executing");

        // Future logic: shield break, swaps, backstab, attack sequences
    }

    // Utility method to show a message above the hotbar
    private void sendHotbarMessage(String message) {
        if (client.player != null) {
            client.player.sendMessage(Text.of(message), true); // 'true' displays over hotbar
        }
    }
}
