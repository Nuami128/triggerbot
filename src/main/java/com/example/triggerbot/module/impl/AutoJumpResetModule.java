package com.example.triggerbot.module.impl;

import com.example.triggerbot.module.EmptyModule;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;

public class AutoJumpResetModule extends EmptyModule {

    private float lastHealth = -1f;

    public AutoJumpResetModule() {
        super("Auto Jump Reset");
    }

    @Override
    public void onTick() {
        MinecraftClient mc = MinecraftClient.getInstance();

        if (mc.player == null || mc.world == null) return;
        if (mc.currentScreen != null) return;
        if (mc.player.isDead()) return;

        // Don't jump reset while eating
        if (mc.player.isUsingItem()) return;

        // Only on ground — no crits, no airborne
        if (!mc.player.isOnGround()) return;

        // Must be sprinting
        if (!mc.player.isSprinting()) return;

        // Must be moving forward
        if (!mc.options.forwardKey.isPressed()) return;

        float currentHealth = mc.player.getHealth();

        // Detect damage taken this tick
        boolean tookDamage = lastHealth > 0 && currentHealth < lastHealth;
        lastHealth = currentHealth;

        if (!tookDamage) return;

        // Must be facing 180° away from attacker
        Entity attacker = findAttacker(mc);
        if (attacker == null) return;
        if (!isFacingAway(mc, attacker)) return;

        // Jump in the same tick as damage — perfect timing
        mc.player.jump();
        mc.player.sendMessage(net.minecraft.text.Text.literal("§aJump Reset"), true);
    }

    // Find the nearest player attacker in reach
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

    // Check if player is facing 180° away from the attacker (within 45° tolerance)
    private boolean isFacingAway(MinecraftClient mc, Entity attacker) {
        // Direction from us to attacker
        double dx = attacker.getX() - mc.player.getX();
        double dz = attacker.getZ() - mc.player.getZ();

        // Angle to attacker
        float angleToAttacker = (float) Math.toDegrees(Math.atan2(-dx, dz));

        // Our yaw
        float ourYaw = MathHelper.wrapDegrees(mc.player.getYaw());
        angleToAttacker = MathHelper.wrapDegrees(angleToAttacker);

        // Difference — 180° means we're facing away
        float diff = Math.abs(MathHelper.wrapDegrees(ourYaw - angleToAttacker));

        // Allow 45° tolerance either side of 180°
        return diff >= 135f && diff <= 225f;
    }
}
