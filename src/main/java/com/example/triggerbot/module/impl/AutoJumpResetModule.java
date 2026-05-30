package com.example.triggerbot.module.impl;

import com.example.triggerbot.module.EmptyModule;
import net.minecraft.client.MinecraftClient;

public class AutoJumpResetModule extends EmptyModule {

    private int lastHurtTime = 0;
    private int hurtLockTicks = 0;

    public AutoJumpResetModule() {
        super("Auto Jump Reset");
    }

    @Override
    public void onTick() {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc == null || mc.player == null) return;

        if (hurtLockTicks > 0) hurtLockTicks--;
        lastHurtTime = mc.player.hurtTime;
    }

    @Override
    public void onPostMovement() {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc == null || mc.player == null) return;

        int hurt = mc.player.hurtTime;

        // Damage edge detect — fire jump same tick damage is received
        if (hurt > 0 && lastHurtTime == 0 && hurtLockTicks == 0) {
            hurtLockTicks = 10;

            // Only jump if on ground
            if (mc.player.isOnGround()) {
                mc.player.jump();
            }
        }

        lastHurtTime = hurt;
    }
}
