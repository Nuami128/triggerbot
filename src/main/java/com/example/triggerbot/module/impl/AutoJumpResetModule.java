package com.example.triggerbot.module.impl;

import com.example.triggerbot.module.EmptyModule;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.math.MathHelper;

public class AutoJumpResetModule extends EmptyModule {

    private boolean damagePending = false;

    public AutoJumpResetModule() {
        super("Auto Jump Reset");
    }

    // Called from mixin when health packet arrives
    public void onDamageTaken() {
        damagePending = true;
    }

    @Override
    public void onTick() {
        if (!damagePending) return;

        MinecraftClient mc = MinecraftClient.getInstance();

        if (mc.player == null || mc.world == null) return;
        if (mc.currentScreen != null) return;
        if (mc.player.isDead()) return;

        damagePending = false;

        if (mc.player.isUsingItem()) {
            mc.player.sendMessage(Text.literal("§cJR: eating"), true);
            return;
        }

        if (!mc.player.isOnGround()) {
            mc.player.sendMessage(Text.literal("§cJR: airborne"), true);
            return;
        }

        // Don't jump if already moving upward
        if (mc.player.getVelocity().y > 0) {
            mc.player.sendMessage(Text.literal("§cJR: already jumping"), true);
            return;
        }

        if (!mc.player.isSprinting()) {
            mc.player.sendMessage(Text.literal("§cJR: not sprinting"), true);
            return;
        }

        double velX = mc.player.getVelocity().x;
        double velZ = mc.player.getVelocity().z;
        if ((velX * velX + velZ * velZ) < 0.001) {
            mc.player.sendMessage(Text.literal("§cJR: not moving"), true);
            return;
        }

        Entity attacker = findAttacker(mc);
        if (attacker == null) {
            mc.player.sendMessage(Text.literal("§cJR: no attacker"), true);
            return;
        }

        if (!isAttackerInFront(mc, attacker)) {
            float diff = getFacingDiff(mc, attacker);
            mc.player.sendMessage(Text.literal("§cJR: wrong angle " + (int) diff + "°"), true);
            return;
        }

        // Just jump — no velocity override
        mc.player.jump();
        mc.player.sendMessage(Text.literal("§aJump Reset"), true);
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

            if (dist < closestDist) {
                closestDist = dist;
                closest = e;
            }
        }

        return closest;
    }

    private float getFacingDiff(MinecraftClient mc, Entity attacker) {
        double dx = attacker.getX() - mc.player.getX();
        double dz = attacker.getZ() - mc.player.getZ();
        float angleToAttacker = MathHelper.wrapDegrees(
                (float) Math.toDegrees(Math.atan2(-dx, dz))
        );
        float ourYaw = MathHelper.wrapDegrees(mc.player.getYaw());
        // Absolute difference clamped to 0-180
        float diff = Math.abs(MathHelper.wrapDegrees(ourYaw - angleToAttacker));
        if (diff > 180f) diff = 360f - diff;
        return diff;
    }

    // Attacker should be in FRONT — diff close to 0° means facing them
    private boolean isAttackerInFront(MinecraftClient mc, Entity attacker) {
        float diff = getFacingDiff(mc, attacker);
        return diff <= 90f;
    }
}
