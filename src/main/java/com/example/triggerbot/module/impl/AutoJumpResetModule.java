package com.example.triggerbot.module.impl;

import com.example.triggerbot.module.EmptyModule;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.math.MathHelper;

public class AutoJumpResetModule extends EmptyModule {

    public AutoJumpResetModule() {
        super("Auto Jump Reset");
    }

    // Called from mixin when health update packet arrives
    public void onDamageTaken() {
        MinecraftClient mc = MinecraftClient.getInstance();

        if (mc.player == null || mc.world == null) return;
        if (mc.currentScreen != null) return;
        if (mc.player.isDead()) return;

        if (mc.player.isUsingItem()) {
            mc.player.sendMessage(Text.literal("§cJR: eating"), true);
            return;
        }

        if (!mc.player.isOnGround()) {
            mc.player.sendMessage(Text.literal("§cJR: airborne"), true);
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

        if (!isFacingAway(mc, attacker)) {
            float diff = getFacingDiff(mc, attacker);
            mc.player.sendMessage(Text.literal("§cJR: wrong angle " + (int) diff + "°"), true);
            return;
        }

        mc.player.jump();
        mc.player.setVelocity(
                mc.player.getVelocity().x,
                0.42f,
                mc.player.getVelocity().z
        );
        mc.player.sendMessage(Text.literal("§aJump Reset"), true);
    }

    @Override
    public void onTick() {}

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
        return Math.abs(MathHelper.wrapDegrees(ourYaw - angleToAttacker));
    }

    private boolean isFacingAway(MinecraftClient mc, Entity attacker) {
        float diff = getFacingDiff(mc, attacker);
        return diff >= 135f && diff <= 225f;
    }
}
