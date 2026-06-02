package com.example.triggerbot.module.impl;

import com.example.triggerbot.module.EmptyModule;
import net.minecraft.client.MinecraftClient;

public class AutoJumpResetModule extends EmptyModule {

    private int cooldown = 0;
    private int pendingJump = -1;
    private int jumpTicksLeft = 0; // Better tracking for holding/releasing the key

    public AutoJumpResetModule() {
        super("Auto Jump Reset");
    }

    @Override
    public void onTick() {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc == null || mc.player == null) return;

        // Decrement cooldowns
        if (cooldown > 0) cooldown--;

        // Input Simulation Worker
        if (jumpTicksLeft > 0) {
            jumpTicksLeft--;
            if (jumpTicksLeft == 0) {
                // Safely release the spacebar after the game processes the physics tick
                mc.options.jumpKey.setPressed(false);
            }
        }

        // Pending Jump Evaluator
        if (pendingJump > 0) {
            pendingJump--;
            if (pendingJump == 0) {
                // Strict vanilla compliance ground-check to pass Grim Anticheat
                if (mc.player.isOnGround()) {
                    // Force the raw input state active
                    mc.options.jumpKey.setPressed(true); 
                    
                    // Minecraft uses the .pressed field for movement inputs inside ClientPlayerEntity
                    // Depending on mappings, this field name is usually just "pressed"
                    mc.options.jumpKey.pressed = true; 

                    jumpTicksLeft = 1; // Keep it held for exactly this tick
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
