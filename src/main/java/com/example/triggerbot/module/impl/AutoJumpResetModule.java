package com.example.triggerbot.module.impl;

import com.example.triggerbot.module.EmptyModule;
import net.minecraft.client.MinecraftClient;

public class AutoJumpResetModule extends EmptyModule {

    private boolean wasHurt = false;
    private boolean shouldJump = false;

    public AutoJumpResetModule() {
        super("Auto Jump Reset");
    }

    @Override
    public void onTick() {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc == null || mc.player == null) return;

        // Force-clear the jump request if we are no longer hurt 
        // OR if we are flying/falling to prevent delayed "ghost" jumps
        if (mc.player.hurtTime == 0 || !mc.player.isOnGround()) {
            shouldJump = false;
        }
    }

    @Override
    public void onJumpReset() {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc == null || mc.player == null) return;
        if (mc.currentScreen != null) return;
        if (mc.player.isUsingItem()) return;

        boolean hurtNow = mc.player.hurtTime > 0;

        // 1. Detect rising edge of damage instantly
        if (hurtNow && !wasHurt) {
            shouldJump = true;
        }
        wasHurt = hurtNow;

        // 2. Fire strictly if we are on the ground at the moment of impact
        if (shouldJump && mc.player.isOnGround()) {
            mc.player.jump();
            shouldJump = false; // Consume the trigger immediately
            System.out.println("[AutoJR] JUMP FIRED SUCCESSFULLY");
        }
    }

    @Override
    public void onPostMovement() {}
}
