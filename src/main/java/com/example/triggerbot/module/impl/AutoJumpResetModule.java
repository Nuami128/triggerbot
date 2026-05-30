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

        // 1. Pure rising edge capture
        if (hurtTime > lastHurtTime && hurtTime > 0) {
            // If we were safe on the block last frame, flag a valid vanilla execution window
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

        // 2. Hardware Input Simulation
        if (shouldJump) {
            // VANILLA INPUT ONLY: We press the game's actual macro binding key.
            // Because this executes at tickMovement HEAD, Minecraft reads this press 
            // a fraction of a millisecond later, executing a native physics jump.
            mc.options.jumpKey.setPressed(true);
            
            shouldJump = false; // Instantly consume the flag to avoid double jumping
            System.out.println("[AutoJR] Vanilla Input Jump Fired!");
        }
    }

    @Override
    public void onPostMovement() {
        // Handled cleanly at tick HEAD via your mixin architecture
    }
}

