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

        // 1. Precise rising edge tracking to catch hits before micro-lifts update states
        if (hurtTime > lastHurtTime && hurtTime > 0) {
            if (mc.player.isOnGround() || wasOnGroundLastTick) {
                shouldJump = true;
            }
        }

        if (hurtTime == 0) {
            shouldJump = false;
        }

        // Cache historical ground status at the absolute end of the tick loop
        wasOnGroundLastTick = mc.player.isOnGround();
        lastHurtTime = hurtTime;
    }

    @Override
    public void onJumpReset() {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc == null || mc.player == null) return;
        if (mc.currentScreen != null || mc.player.isUsingItem()) return;

        // 2. Direct Engine Injection executed cleanly within your TAIL window
        if (shouldJump) {
            // ANTI-CHEAT BYPASS: Mark the player entity state fields as actively jumping.
            // This satisfies server prediction models and stops "Simulation" discrepancies.
            mc.player.setJumping(true);
            
            // PHYSICS ENGINE UPDATE: Instantly alter velocity vectors on this execution frame
            // to slash your horizontal knockback and apply your vertical pop.
            mc.player.jump();
            
            shouldJump = false; // Instantly consume the flag to prevent double jumping
            System.out.println("[AutoJR] Instant Physics Reset Executed Safely.");
        }
    }

    @Override
    public void onPostMovement() {
        // Safe at tick HEAD via your custom mixin setup
    }
}
