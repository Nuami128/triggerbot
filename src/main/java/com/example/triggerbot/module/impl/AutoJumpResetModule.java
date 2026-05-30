package com.example.triggerbot.module.impl;

import com.example.triggerbot.module.EmptyModule;
import net.minecraft.client.MinecraftClient;

public class AutoJumpResetModule extends EmptyModule {

    private boolean triggerJumpNextFrame = false;
    private int cooldown = 0;

    public AutoJumpResetModule() {
        super("Auto Jump Reset");
    }

    @Override
    public void onJumpReset() {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc == null || mc.player == null) return;
        if (mc.currentScreen != null || mc.player.isUsingItem()) return;

        // Step down the cooldown timer on each physics loop processing step
        if (cooldown > 0) {
            cooldown--;
        }

        // FIXED: Replaced strict == 10 with your flexible, lag-resilient damage-state condition
        if (mc.player.hurtTime > 0 && cooldown == 0) {
            triggerJumpNextFrame = true;
            cooldown = 10; // Lock out detection for 10 ticks to prevent duplicate triggers
        }
    }

    @Override
    public void onPostMovement() {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc == null || mc.player == null) return;

        // The trigger flag stays primed until a valid grounded jump successfully completes
        if (triggerJumpNextFrame) {
            if (mc.player.isOnGround()) {
                mc.player.jump();
                System.out.println("[AutoJR] Jump triggered");
                triggerJumpNextFrame = false;
            }
        }
    }

    @Override
    public void onTick() {}
}

