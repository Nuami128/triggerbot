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

        // detection
        if (mc.player.hurtTime > 0 && cooldown == 0) {
            triggerJumpNextFrame = true;
            cooldown = 10;
        }
    }

    @Override
    public void onPostMovement() {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc == null || mc.player == null) return;

        // execution
        if (triggerJumpNextFrame) {
            mc.player.jump();
            triggerJumpNextFrame = false;
            System.out.println("[AutoJR] Jump triggered");
        }
    }

    @Override
    public void onTick() {}
}

