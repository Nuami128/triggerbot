package com.example.triggerbot.module.impl;

import com.example.triggerbot.module.EmptyModule;
import net.minecraft.client.MinecraftClient;

public class AutoJumpResetModule extends EmptyModule {

    private int lastHurtTime = 0;
    private boolean shouldJump = false;
    private int pressHoldTicks = 0; // Tracks how long the spacebar stays held down

    public AutoJumpResetModule() {
        super("Auto Jump Reset");
    }

    @Override
    public void onTick() {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc == null || mc.player == null) return;

        // 1. Keep the key held down across ticks to match true human hardware timing
        if (pressHoldTicks > 0) {
            pressHoldTicks--;
            if (pressHoldTicks == 0) {
                mc.options.jumpKey.setPressed(false); // Finally release the spacebar
            }
        }

        int hurtTime = mc.player.hurtTime;

        // 2. Persistent state tracking for damage onset
        if (hurtTime > lastHurtTime && hurtTime > 0) {
            if (mc.player.isOnGround()) {
                shouldJump = true;
            }
        }

        // Safety cleanup if the damage tracking resets completely
        if (hurtTime == 0) {
            shouldJump = false;
        }

        lastHurtTime = hurtTime;
    }

    @Override
    public void onJumpReset() {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc == null || mc.player == null) return;
        if (mc.currentScreen != null || mc.player.isUsingItem()) return;

        // 3. Force the physical hold window
        if (shouldJump && mc.player.isOnGround()) {
            mc.options.jumpKey.setPressed(true);
            pressHoldTicks = 2; // Hold it down for 2 ticks to guarantee the physics loop catches it
            
            shouldJump = false; // Consume the trigger flag
            System.out.println("[AutoJR] Persistent Jump Input Injected!");
        }
    }

    @Override
    public void onPostMovement() {
        // Safe to use for your combat strafes at tick HEAD
    }
}
