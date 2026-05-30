package com.example.triggerbot.module.impl;

import com.example.triggerbot.module.EmptyModule;
import net.minecraft.client.MinecraftClient;

public class AutoJumpResetModule extends EmptyModule {

    private int lastHurtTime = 0;
    private boolean shouldJump = false;
    private boolean wasOnGroundLastTick = true;

    public AutoJumpResetModule() {
        super("Auto Jump Reset");
    }

    @Override
    public void onTick() {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc == null || mc.player == null) return;

        int hurtTime = mc.player.hurtTime;

        // 1. Rising edge check using current and historical frames to catch micro-lifts
        if (hurtTime > lastHurtTime && hurtTime > 0) {
            if (mc.player.isOnGround() || wasOnGroundLastTick) {
                shouldJump = true;
            }
        }

        if (hurtTime == 0) {
            shouldJump = false;
        }

        // Cache true ground status at the very end of the tick loop
        wasOnGroundLastTick = mc.player.isOnGround();
        lastHurtTime = hurtTime;
    }

    @Override
    public void onJumpReset() {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc == null || mc.player == null) return;
        if (mc.currentScreen != null || mc.player.isUsingItem()) return;

        // 2. Native Method Injection inside the hasMovementInput() window
        if (shouldJump) {
            // This vanilla method updates the player's engine state cleanly, 
            // bypassing abstract Input field mapping errors completely.
            mc.player.setJumping(true);
            
            // Trigger the native physics jump logic block inside the loop
            mc.player.jump();
            
            shouldJump = false; // Instantly consume the flag to prevent looping jumps
            System.out.println("[AutoJR] Native Jump Reset Triggered.");
        }
    }

    @Override
    public void onPostMovement() {
        // Safe at tick HEAD via your custom mixin setup
    }
}
