package com.example.triggerbot.module.impl;

import com.example.triggerbot.module.EmptyModule;
import net.minecraft.client.MinecraftClient;

public class AutoJumpResetModule extends EmptyModule {

    private int lastHurtTime = 0;
    private int hurtLockTicks = 0;
    private boolean shouldJump = false;
    private boolean releaseJump = false;

    public AutoJumpResetModule() {
        super("Auto Jump Reset");
    }

    @Override
    public void onTick() {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc == null || mc.player == null || mc.world == null) return;

        int hurt = mc.player.hurtTime;

        if (hurt > lastHurtTime && hurt >= 9 && hurtLockTicks == 0) {
            hurtLockTicks = 20;
            shouldJump = true;
        }

        if (hurtLockTicks > 0) hurtLockTicks--;

        lastHurtTime = hurt;
    }

    @Override
    public void onJumpReset() {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc == null || mc.player == null) return;

        if (releaseJump) {
            mc.player.input.jumping = false;
            releaseJump = false;
        }

        if (!shouldJump) return;
        shouldJump = false;

        if (mc.world == null) return;

        mc.player.input.jumping = true;
        releaseJump = true;
        System.out.println("[AutoJR] JUMP FIRED");
    }

    @Override
    public void onPostMovement() {}
}
