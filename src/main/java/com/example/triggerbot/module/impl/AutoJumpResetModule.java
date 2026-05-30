package com.example.triggerbot.module.impl;

import com.example.triggerbot.module.EmptyModule;
import net.minecraft.client.MinecraftClient;

public class AutoJumpResetModule extends EmptyModule {

    private int lastHurtTime = 0;
    private boolean shouldJump = false;
    private int releaseTickTimer = 0;

    public AutoJumpResetModule() {
        super("Auto Jump Reset");
    }

    @Override
    public void onTick() {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc == null || mc.player == null) return;

        // 1. Unpress the key on the subsequent tick to simulate a real hardware press
        if (releaseTickTimer > 0) {
            releaseTickTimer--;
            if (releaseTickTimer == 0) {
                mc.options.jumpKey.setPressed(false);
            }
        }

        int hurtTime = mc.player.hurtTime;

        // 2. Rising edge state tracking
        if (hurtTime > lastHurtTime) {
            shouldJump = true;
        }

        // Safety reset: Clear flag if the animation ended or we are already airborne
        if (hurtTime == 0 || !mc.player.isOnGround()) {
            shouldJump = false;
        }

        lastHurtTime = hurtTime;
    }

    @Override
    public void onJumpReset() {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc == null || mc.player == null) return;
        if (mc.currentScreen != null) return;
        if (mc.player.isUsingItem()) return;

        // 3. Trigger via Vanilla Inputs to bypass Prediction/Simulation checks
        if (shouldJump && mc.player.isOnGround()) {
            mc.options.jumpKey.setPressed(true); // Let Minecraft's motor engine handle the physics safely
            releaseTickTimer = 1;                // Schedule key release
            shouldJump = false;                  // Consume the trigger
            System.out.println("[AutoJR] Safe Input Jump Fired");
        }
    }

    @Override
    public void onPostMovement() {}
}
