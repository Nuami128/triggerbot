package com.example.triggerbot.module.impl;

import com.example.triggerbot.module.EmptyModule;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.math.MathHelper;

public class AutoJumpResetModule extends EmptyModule {

    private boolean damagePending = false;
    private int lastHurtTime = 0;

    public AutoJumpResetModule() {
        super("Auto Jump Reset");
    }

    // Called from mixin at exact moment damage lands
    public void onDamageTaken() {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player == null || mc.world == null) return;
        if (mc.player.isUsingItem()) return;
        if (!mc.player.isOnGround()) return;
        if (mc.player.getVelocity().y > 0) return;
        if (!mc.player.isSprinting()) return;

        double velX = mc.player.getVelocity().x;
        double velZ = mc.player.getVelocity().z;
        if ((velX * velX + velZ * velZ) < 0.001) return;

        Entity attacker = findAttacker(mc);
        if (attacker == null) return;
        if (!isAttackerInFront(mc, attacker)) return;

        mc.player.jump();
        mc.player.sendMessage(Text.literal("§aJump Reset"), true);
    }

    @Override
    public void onTick() {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player == null || mc.world == null) return;
        if (mc.currentScreen != null) return;
        if (mc.player.isDead()) return;

        int hurtTime = mc.player.hurtTime;

        // Fallback — retry if first attempt failed
        if (hurtTime > 0 && lastHurtTime == 0) {
            damagePending = true;
        }
        lastHurtTime = hurtTime;

        if (!damagePending) return;
        if (!mc.player.isOnGround()) return;
        if (mc.player.getVelocity().y > 0) return;
        if (!mc.player.isSprinting()) return;

        double velX = mc.player.getVelocity().x;
        double velZ = mc.player.getVelocity().z;
        if ((velX * velX + velZ * velZ) < 0.001) return;

        Entity attacker = findAttacker(mc);
        if (attacker == null) { damagePending = false; return; }
        if (!isAttackerInFront(mc, attacker)) { damagePending = false; return; }

        damagePending = false;
        mc.player.jump();
        mc.player.sendMessage(Text.literal("§aJump Reset retry"), true);
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
        float diff = Math.abs(MathHelper.wrapDegrees(ourYaw - angleToAttacker));
        if (diff > 180f) diff = 360f - diff;
        return diff;
    }

    private boolean isAttackerInFront(MinecraftClient mc, Entity attacker) {
        float diff = getFacingDiff(mc, attacker);
        return diff <= 90f;
    }
}
