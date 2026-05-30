package com.example.triggerbot.module.impl;

import com.example.triggerbot.module.EmptyModule;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.Box;

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

        // Release jump key from previous tick
        if (releaseJump) {
            mc.options.jumpKey.setPressed(false);
            releaseJump = false;
        }

        if (!shouldJump) return;
        shouldJump = false;

        if (mc.world == null) return;

        if (playerWithinRange(mc, 4.0)) {
            mc.options.jumpKey.setPressed(true);
            releaseJump = true;
            System.out.println("[AutoJR] JUMP FIRED");
        } else {
            System.out.println("[AutoJR] SKIPPED - no player in range");
        }
    }

    private boolean playerWithinRange(MinecraftClient mc, double range) {
        Box searchBox = mc.player.getBoundingBox().expand(range);
        for (var entity : mc.world.getEntitiesByClass(
                PlayerEntity.class, searchBox,
                e -> e != mc.player && e.isAlive() && !e.isSpectator())) {
            return true;
        }
        return false;
    }

    @Override
    public void onPostMovement() {}
}
