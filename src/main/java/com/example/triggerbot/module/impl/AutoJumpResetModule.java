package com.example.triggerbot.module.impl;

import com.example.triggerbot.module.EmptyModule;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.input.KeyboardInput;

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

        // 2. Client Input Override System
        if (shouldJump) {
            // ANTI-CHEAT SIMULATION BYPASS:
            // We safely cast 'input' to 'KeyboardInput' to let the compiler resolve fields correctly.
            if (mc.player.input instanceof KeyboardInput input) {
                input.jumping = true;
                
                // Keep directional vectors synchronized to avoid prediction mismatches
                if (mc.options.forwardKey.isPressed()) input.movementForward = 1.0F;
                if (mc.options.backKey.isPressed()) input.movementForward = -1.0F;
                if (mc.options.leftKey.isPressed()) input.movementSideways = 1.0F;
                if (mc.options.rightKey.isPressed()) input.movementSideways = -1.0F;
            }

            // Simultaneously tell the base entity engine that we are jumping
            mc.player.setJumping(true);
            
            // Also press the hardware options key to guarantee correct outgoing packet structures
            mc.options.jumpKey.setPressed(true);
            
            shouldJump = false; // Instantly consume the flag to prevent looping jumps
            System.out.println("[AutoJR] Clean Input Stream Spoofed Successfully.");
        }
    }

    @Override
    public void onPostMovement() {
        // Safe at tick HEAD via your custom mixin setup
    }
}
