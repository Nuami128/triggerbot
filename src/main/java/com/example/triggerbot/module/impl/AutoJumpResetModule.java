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

        // 2. Direct Engine Input Injection inside the hasMovementInput() window
        if (shouldJump) {
            // Explicitly cast to KeyboardInput to resolve the "cannot find symbol" Gradle compiler error
            if (mc.player.input instanceof KeyboardInput) {
                KeyboardInput input = (KeyboardInput) mc.player.input;
                
                // Force the native jumping flag true inside the engine physics sequence
                input.jumping = true;
                
                // ANTI-CHEAT SIMULATION BYPASS: Ensure that if you are moving, the jump request
                // explicitly mimics real keyboard states so GrimAC doesn't detect raw input anomalies.
                if (mc.options.forwardKey.isPressed()) input.movementForward = 1.0F;
                if (mc.options.backKey.isPressed()) input.movementForward = -1.0F;
                if (mc.options.leftKey.isPressed()) input.movementSideways = 1.0F;
                if (mc.options.rightKey.isPressed()) input.movementSideways = -1.0F;
            }
            
            shouldJump = false; // Instantly consume the flag to prevent looping jumps
            System.out.println("[AutoJR] Native Input Overwritten within hasMovementInput Pipeline.");
        }
    }

    @Override
    public void onPostMovement() {
        // Safe at tick HEAD via your custom mixin setup
    }
}
