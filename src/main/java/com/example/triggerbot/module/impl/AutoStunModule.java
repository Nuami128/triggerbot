package com.example.triggerbot.module.impl;

import com.example.triggerbot.module.ClientModule; // make sure this path is correct
import net.minecraft.client.MinecraftClient;
import net.minecraft.text.Text;

public class AutoStunModule implements ClientModule {

    private final MinecraftClient client = MinecraftClient.getInstance();
    private boolean enabled = false;

    // ----------------------------
    // Implemented interface methods
    // ----------------------------

    @Override
    public String getName() {
        return "Auto Stun";
    }

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
    public void tick() {
        if (!enabled) return;

        // Debug tick message
        System.out.println("AutoStunModule tick executing");

        // Future logic:
        // - Shield break
        // - Axe/Sword swap sequences
        // - Backstab/front logic
        // - Tick-queued attacks
    }

    // ----------------------------
    // Utility methods
    // ----------------------------
    
    private void sendHotbarMessage(String message) {
        if (client.player != null) {
            client.player.sendMessage(Text.of(message), true); // 'true' displays over hotbar
        }
    }
}
