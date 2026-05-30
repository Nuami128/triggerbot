package com.example.triggerbot.module.impl;

import com.example.triggerbot.module.EmptyModule;
import net.minecraft.client.MinecraftClient;

public class AutoJumpResetModule extends EmptyModule {

    private int lastHurtTime = 0;
    private boolean shouldJump = false;
    private boolean wasOnGroundLastTick = true;
    private int pressHoldTimer = 0;

    public AutoJumpResetModule() {
        super("Auto Jump Reset");
    }

    @Override
    public void onTick() {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc == null || mc.player == null) return;

        // 1. Release the key bind exactly one tick later to mimic clean human input streams
        if (pressHoldTimer > 0) {
            pressHoldTimer--;
            if (pressHoldTimer == 0) {
                mc.options.jumpKey.setPressed(false);
            }
        }

        int hurtTime = mc.player.hurtTime;

        // 2. Rising edge tracking using current and historical states
        if (hurtTime > lastHurtTime && hurtTime > 0) {
            if (mc.player.isOnGround() || wasOnGroundLastTick) {
                shouldJump = true;
            }
        }

        if (hurtTime == 0) {
            shouldJump = false;
        }

        wasOnGroundLastTick = mc.player.isOnGround();
        lastHurtTime = hurtTime;
    }

    @Override
    public void onJumpReset() {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc == null || mc.player == null) return;
        if (mc.currentScreen != null || mc.player.isUsingItem()) return;

        // 3. Input Spoofer Loop inside the hasMovementInput() window
        if (shouldJump) {
            // BYPASS: We completely remove mc.player.jump() and mc.player.setJumping(true).
            // By setting the actual options state true inside this precise mixin window,
            // Minecraft natively triggers the jumping logic checks on the next instruction frame.
            // This eliminates raw vector injection anomalies, bypassing 180-degree simulation flags.
            mc.options.jumpKey.setPressed(true);
            pressHoldTimer = 1; // Schedule key release for the subsequent tick
            
            shouldJump = false; // Instantly consume the flag to prevent looping jumps
            System.out.println("[AutoJR] Hardware Input Simulated within hasMovementInput.");
        }
    }

    @Override
    public void onPostMovement() {
        // Safe at tick HEAD via your custom mixin setup
    }
}
