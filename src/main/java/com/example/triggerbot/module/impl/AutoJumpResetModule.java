package com.example.triggerbot.module.impl;

import com.example.triggerbot.module.EmptyModule;
import net.minecraft.client.MinecraftClient;

public class AutoJumpResetModule extends EmptyModule {

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

        // Detect active damage state and immediately inject raw jump velocity
        if (mc.player.hurtTime > 0 && cooldown == 0) {
            mc.player.jump(); // execute immediately
            cooldown = 10;
            System.out.println("[AutoJR] Jump triggered");
        }
    }

    @Override
    public void onTick() {}

    @Override
    public void onPostMovement() {}
}
