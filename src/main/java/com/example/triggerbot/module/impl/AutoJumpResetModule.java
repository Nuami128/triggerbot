package com.example.triggerbot.module.impl;

import com.example.triggerbot.module.EmptyModule;
import net.minecraft.client.MinecraftClient;

public class AutoJumpResetModule extends EmptyModule {

    private int cooldown = 0;
    private int pendingJump = -1;
    private boolean isJumping = false; // Tracks if we are actively holding space

    public AutoJumpResetModule() {
        super("Auto Jump Reset");
    }

    @Override
    public void onTick() {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc == null || mc.player == null) return;

        // Release the key on the tick immediately following the jump trigger
        if (isJumping) {
            mc.options.jumpKey.setPressed(false);
            isJumping = false;
        }

        if (cooldown > 0) cooldown--;

        if (pendingJump > 0) {
            pendingJump--;
            if (pendingJump == 0) {
                // Grim requires strict ground verification when the key is pressed
                if (mc.player.isOnGround()) {
                    mc.options.jumpKey.setPressed(true); 
                    isJumping = true; // Flag to release it on the next tick
                    cooldown = 10;
                }
                pendingJump = -1;
            }
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

    @Override public void onClientTick() {}
    @Override public void onJumpReset() {}
    @Override public void onPostMovement() {}
    @Override public void onDamage() {}
}

