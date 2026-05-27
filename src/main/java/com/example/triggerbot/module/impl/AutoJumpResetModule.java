package com.example.triggerbot.module.impl;

import com.example.triggerbot.module.EmptyModule;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.math.MathHelper;

public class AutoJumpResetModule extends EmptyModule {

    private int prevHurtTime = 0;
    private int pendingJumpTicks = 0;
    private int jumpHeldTicks = 0;

    public AutoJumpResetModule() {
        super("Auto Jump Reset");
    }

    private void onHitReceived(MinecraftClient mc) {
        if (mc.player == null || mc.world == null) return;
        if (mc.player.isUsingItem()) { debug(mc, "skip: eating"); return; }
        if (!mc.player.isSprinting()) { debug(mc, "skip: not sprinting"); return; }

        double velX = mc.player.getVelocity().x;
        double velZ = mc.player.getVelocity().z;
        if ((velX * velX + velZ * velZ) < 0.001) { debug(mc, "skip: not moving"); return; }

        Entity attacker = findAttacker(mc);
        if (attacker == null) { debug(mc, "skip: no attacker"); return; }
        if (getFacingDiff(mc, attacker) > 90f) { debug(mc, "skip: wrong angle"); return; }

        pendingJumpTicks = 5;
        debug(mc, "jump pending");
    }

    @Override
    public void onTick() {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc == null || mc.player == null) { prevHurtTime = 0; return; }

        // Hold jump key for 2 ticks then release
        if (jumpHeldTicks > 0) {
            mc.options.jumpKey.setPressed(true);
            jumpHeldTicks--;
            if (jumpHeldTicks == 0) {
                mc.options.jumpKey.setPressed(false);
            }
        }

        // Detect new hit
        int hurtTime = mc.player.hurtTime;
        if (hurtTime > prevHurtTime && prevHurtTime <= 1) {
            System.out.println("[TriggerBot] Hit! hurtTime=" + hurtTime);
            onHitReceived(mc);
        }
        prevHurtTime = hurtTime;

        // Fire jump the tick we land
        if (pendingJumpTicks > 0) {
            pendingJumpTicks--;
            if (mc.player.isOnGround()) {
                debug(mc, "FIRED");
                System.out.println("[TriggerBot] jump fired");
                // Hold the key for 2 ticks — simulates a real press+release
                jumpHeldTicks = 2;
                mc.options.jumpKey.setPressed(true);
                pendingJumpTicks = 0;
            } else if (pendingJumpTicks == 0) {
                debug(mc, "skip: never landed");
            }
        }
    }

    private void debug(MinecraftClient mc, String msg) {
        if (mc.player != null)
            mc.player.sendMessage(Text.literal("JR: " + msg), true);
    }

    private Entity findAttacker(MinecraftClient mc) {
        Entity closest = null;
        double closestDist = Double.MAX_VALUE;
        for (Entity e : mc.world.getEntities()) {
            if (!(e instanceof PlayerEntity)) continue;
            if (e == mc.player) continue;
            if (!e.isAlive()) continue;
            if (e.isRemoved()) continue;
            if (e.isSpectator()) continue;
            double dx = mc.player.getX() - e.getX();
            double dy = mc.player.getY() - e.getY();
            double dz = mc.player.getZ() - e.getZ();
            double dist = dx * dx + dy * dy + dz * dz;
            if (dist < closestDist) { closestDist = dist; closest = e; }
        }
        return closest;
    }

    private float getFacingDiff(MinecraftClient mc, Entity attacker) {
        double dx = attacker.getX() - mc.player.getX();
        double dz = attacker.getZ() - mc.player.getZ();
        float angleToAttacker = MathHelper.wrapDegrees(
                (float) Math.toDegrees(Math.atan2(-dx, dz)));
        float ourYaw = MathHelper.wrapDegrees(mc.player.getYaw());
        float diff = Math.abs(MathHelper.wrapDegrees(ourYaw - angleToAttacker));
        if (diff > 180f) diff = 360f - diff;
        return diff;
    }
}
