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

        // 1. Precise rising-edge tracking using historical states to capture hits instantly
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

        // 2. Pure Public Input Assignment
        if (shouldJump) {
            // FIX: We rely exclusively on clear, public, method bindings that are 
            // 100% guaranteed to exist in Fabric 1.21.11 Yarn environments.
            mc.options.jumpKey.setPressed(true);
            mc.player.setJumping(true);
            
            shouldJump = false; // Instantly consume the flag to prevent looping jumps
            System.out.println("[AutoJR] Hardware Input Simulated and Method Injected.");
        }
    }

    @Override
    public void onPostMovement() {
        // Safe at tick HEAD via your custom mixin setup
    }
}
