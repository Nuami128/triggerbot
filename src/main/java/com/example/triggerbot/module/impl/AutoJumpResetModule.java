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

        // 1. Rising edge check using both current and previous tick states
        if (hurtTime > lastHurtTime && hurtTime > 0) {
            if (mc.player.isOnGround() || wasOnGroundLastTick) {
                shouldJump = true;
            }
        }

        if (hurtTime == 0) {
            shouldJump = false;
        }

        // Cache the true ground status for the next tick frame
        wasOnGroundLastTick = mc.player.isOnGround();
        lastHurtTime = hurtTime;
    }

    @Override
    public void onJumpReset() {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc == null || mc.player == null) return;
        if (mc.currentScreen != null || mc.player.isUsingItem()) return;

        // 2. Direct Engine Injection inside the hasMovementInput() loop
        if (shouldJump) {
            // Bypass the keyboard buffer entirely. Execute the official physics jump 
            // right here before the game engine processes friction and backward knockback.
            mc.player.input.jumping = true;
            
            shouldJump = false; // Instantly consume the trigger
            System.out.println("[AutoJR] Direct Engine Jump Forced at hasMovementInput!");
        }
    }

    @Override
    public void onPostMovement() {
        // Safe at tick HEAD via your mixin setup
    }
}
