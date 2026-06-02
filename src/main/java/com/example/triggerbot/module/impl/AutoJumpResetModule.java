package com.example.triggerbot.module.impl;

import com.example.triggerbot.module.EmptyModule;
import net.minecraft.client.MinecraftClient;

public class AutoJumpResetModule extends EmptyModule {

    private int cooldown = 0;
    private int pendingJump = -1;
    private int jumpTicksLeft = 0;

    public AutoJumpResetModule() {
        super("Auto Jump Reset");
    }

    @Override
    public void onTick() {
        // Decrement cooldowns
        if (cooldown > 0) cooldown--;

        // Pending Jump Evaluator
        if (pendingJump > 0) {
            pendingJump--;
            if (pendingJump == 0) {
                MinecraftClient mc = MinecraftClient.getInstance();
                if (mc != null && mc.player != null && mc.player.isOnGround()) {
                    // Set the flag to hold the jump key down
                    jumpTicksLeft = 1; 
                    cooldown = 10;
                }
                pendingJump = -1;
            }
        }
    }

    @Override
    public void onClientTick() {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc == null || mc.player == null) return;

        // Input Simulation Worker - Executing inside the proper client tick loop
        if (jumpTicksLeft > 0) {
            mc.options.jumpKey.setPressed(true);
            jumpTicksLeft--;
        } else {
            // Releasing the key when the timer runs out
            mc.options.jumpKey.setPressed(false);
        }
    }

    @Override
    public void onAttack() {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc == null || mc.player == null) return;
        if (!isEnabled()) return;
        if (!mc.player.isOnGround()) return;
        if (mc.targetedEntity == null) return;
        if (cooldown > 0) return;

        // Random 1-3 tick delay before jumping
        pendingJump = 1 + (int)(Math.random() * 3);
    }

    @Override public void onJumpReset() {}
    @Override public void onPostMovement() {}
    @Override public void onDamage() {}
}
