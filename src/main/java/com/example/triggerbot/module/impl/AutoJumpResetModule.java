package com.example.triggerbot.module.impl;

import com.example.triggerbot.module.EmptyModule;
import net.minecraft.client.MinecraftClient;

public class AutoJumpResetModule extends EmptyModule {

    private int lastHurtTime = 0;
    private int spacebarHoldTimer = 0;

    public AutoJumpResetModule() {
        super("Auto Jump Reset");
    }

    @Override
    public void onJumpReset() {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc == null || mc.player == null) return;

        // 1. Hardware Release Emulation: Simulates lifting your finger off Space
        if (spacebarHoldTimer > 0) {
            spacebarHoldTimer--;
            if (spacebarHoldTimer == 0) {
                mc.options.jumpKey.setPressed(false);
                System.out.println("[AutoJR] Spacebar Released.");
            }
        }

        if (mc.currentScreen != null || mc.player.isUsingItem()) return;

        int currentHurtTime = mc.player.hurtTime;

        // 2. Rising-Edge Interceptor: Catches the exact frame damage registers
        if (currentHurtTime > lastHurtTime && currentHurtTime > 0) {
            if (mc.player.isOnGround()) {
                
                // 3. PURE SPACEBAR INPUT: We override the hardware flag directly.
                // Because this executes at post-tick TAIL, the client engine is forced 
                // to interpret this as a genuine user keystroke on the next physics frame.
                mc.options.jumpKey.setPressed(true);
                spacebarHoldTimer = 2; // Hold down across the tick boundary to ensure it executes
                
                System.out.println("[AutoJR] Simulated Spacebar Pressed Successfully!");
            }
        }

        lastHurtTime = currentHurtTime;
    }

    @Override
    public void onTick() {}

    @Override
    public void onPostMovement() {}
}
