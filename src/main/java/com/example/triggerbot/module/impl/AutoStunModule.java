package com.example.triggerbot.module.impl;

import com.example.triggerbot.module.ClientModule;
import net.minecraft.client.MinecraftClient;
import net.minecraft.text.Text;

public class AutoStunModule implements ClientModule {

    private final MinecraftClient client = MinecraftClient.getInstance();
    private boolean enabled = false;

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
    public void onTick() {
        if (!enabled) return;

        // TODO: add shield-break / axe-sword logic here
        System.out.println("AutoStunModule onTick running"); // debug
    }

    private void sendHotbarMessage(String message) {
        if (client.player != null) {
            client.player.sendMessage(Text.of(message), true); // shows over hotbar
        }
    }
}
