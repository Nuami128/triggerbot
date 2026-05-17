package com.example.triggerbot.module.impl;

import com.example.triggerbot.module.Module;
import net.minecraft.client.MinecraftClient;
import net.minecraft.text.Text;

public class AutoStunModule extends Module {

    private final MinecraftClient client = MinecraftClient.getInstance();
    private boolean enabled = false;

    @Override
    public void onEnable() {
        enabled = true;
        sendHotbarMessage("Auto Stun Enabled");
    }

    @Override
    public void onDisable() {
        enabled = false;
        sendHotbarMessage("Auto Stun Disabled");
    }

    @Override
    public void onTick() {
        if (!enabled) return;

        // Debug tick message - remove when adding full logic
        System.out.println("AutoStunModule tick executing");

        // Future logic: shield break, swaps, backstab, attack sequences
    }

    /**
     * Utility method to show a message above the hotbar
     */
    private void sendHotbarMessage(String message) {
        if (client.player != null) {
            client.player.sendMessage(Text.of(message), true); // 'true' displays over hotbar
        }
    }
}
