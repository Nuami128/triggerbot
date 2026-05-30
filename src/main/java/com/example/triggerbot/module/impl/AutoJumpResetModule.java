package com.example.triggerbot.module.impl;

import com.example.triggerbot.module.EmptyModule;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.Box;

public class AutoJumpResetModule extends EmptyModule {

    private int lastHurtTime = 0;
    private int hurtLockTicks = 0;
    private boolean shouldJump = false;

    public AutoJumpResetModule() {
        super("Auto Jump Reset");
    }

    @Override
    public void onTick() {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc == null || mc.player == null || mc.world == null) return;

        if (hurtLockTicks > 0) hurtLockTicks--;

        int hurt = mc.player.hurtTime;

        if (hurt > lastHurtTime && hurt >= 9 && hurtLockTicks == 0) {
            hurtLockTicks = 10;
            shouldJump = true;
        }

        lastHurtTime = hurt;
    }

    @Override
    public void onJumpReset() {
        if (!shouldJump) return;
        shouldJump = false;

        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc == null || mc.player == null || mc.world == null) return;

        if (!playerWithinRange(mc, 2.5)
                && playerWithinRange(mc, 4.0)
                && mc.targetedEntity == null) {
            mc.player.jump();
            System.out.println("[AutoJR] JUMP FIRED");
        } else {
            System.out.println("[AutoJR] SKIPPED"
                + " noClose=" + !playerWithinRange(mc, 2.5)
                + " inRange=" + playerWithinRange(mc, 4.0)
                + " notTargeting=" + (mc.targetedEntity == null));
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
