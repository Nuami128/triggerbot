package com.example.triggerbot.module.impl;

import com.example.triggerbot.module.EmptyModule;
import net.minecraft.client.MinecraftClient;

public class AutoJumpResetModule extends EmptyModule {

    private boolean pendingJump = false;
    private int lastHurtTime = 0;
    private int hurtLockTicks = 0;

    public AutoJumpResetModule() {
        super("Auto Jump Reset");
    }

    @Override
    public void onTick() {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc == null || mc.player == null) return;

        int hurt = mc.player.hurtTime;

        if (hurtLockTicks > 0) hurtLockTicks--;

        // Damage edge detect — queue the jump, don't fire yet
        if (hurt > 0 && lastHurtTime == 0 && hurtLockTicks == 0) {
            pendingJump = true;
            hurtLockTicks = 10;
        }

        lastHurtTime = hurt;
    }

    @Override
    public void onPostMovement() {
        if (!pendingJump) return;
        pendingJump = false;

        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc == null || mc.player == null) return;
        if (!mc.player.isOnGround()) return;

        mc.player.jump();
    }
}
