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
        if (mc == null || mc.player == null || mc.player.input == null) return;
        if (mc.currentScreen != null || mc.player.isUsingItem()) return;

        // 2. Direct Input Pipeline Injection
        if (shouldJump) {
            // ANTI-CHEAT BYPASS: We write directly to the core player input fields.
            // This forces the client physics engine to execute a vanilla jump
            // while automatically keeping all movement packets 100% legal.
            mc.player.input.jumping = true;
            
            // Re-inject current movement vectors to ensure consistency
            if (mc.options.forwardKey.isPressed()) mc.player.input.movementForward = 1.0F;
            if (mc.options.backKey.isPressed()) mc.player.input.movementForward = -1.0F;
            if (mc.options.leftKey.isPressed()) mc.player.input.movementSideways = 1.0F;
            if (mc.options.rightKey.isPressed()) mc.player.input.movementSideways = -1.0F;
            
            // Mark the player entity state fields as actively jumping
            mc.player.setJumping(true);
            
            shouldJump = false; // Instantly consume the flag to prevent looping jumps
            System.out.println("[AutoJR] Native Input Matrix Written Successfully.");
        }
    }

    @Override
    public void onPostMovement() {
        // Safe at tick HEAD via your custom mixin setup
    }
}
