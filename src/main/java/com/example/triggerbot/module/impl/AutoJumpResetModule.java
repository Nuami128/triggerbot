package com.example.triggerbot.module.impl;

import com.example.triggerbot.module.EmptyModule;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.math.MathHelper;

public class AutoJumpResetModule extends EmptyModule {

    // hurtTime counts down from 10 to 0 after a hit.
    // We detect the moment it jumps back up to 10 (a new hit).
    private int prevHurtTime = 0;
    private int jumpKeyPressTicksRemaining = 0;

    public AutoJumpResetModule() {
        super("Auto Jump Reset");
    }

    // onDamageTaken() is now called from onTick() instead of from a mixin.
    // The mixin on LivingEntity.damage() is removed entirely.
    public void onDamageTaken() {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player == null || mc.world == null) return;

        if (mc.player.isUsingItem()) { debug(mc, "eating"); return; }
        if (!mc.player.isOnGround()) { debug(mc, "airborne"); return; }
        if (mc.player.getVelocity().y > 0) { debug(mc, "ascending"); return; }
        if (!mc.player.isSprinting()) { debug(mc, "not sprinting"); return; }

        double velX = mc.player.getVelocity().x;
        double velZ = mc.player.getVelocity().z;
        if ((velX * velX + velZ * velZ) < 0.001) { debug(mc, "not moving"); return; }

        Entity attacker = findAttacker(mc);
        if (attacker == null) { debug(mc, "no attacker"); return; }
        if (!isAttackerInFront(mc, attacker)) {
            debug(mc, "wrong angle " + (int) getFacingDiff(mc, attacker) + "deg");
            return;
        }

        // Strategy 1: direct velocity jump (most compatible)
        mc.player.jump();

        // Strategy 2: key event simulation — uncomment if jump() doesn't register
        // with the jump reset counter mod. Comment out jump() above first.
        // jumpKeyPressTicksRemaining = 2;
        // mc.options.jumpKey.setPressed(true);

        debug(mc, "Fired!");
        System.out.println("[TriggerBot] AutoJumpReset: jump fired");
    }

    @Override
    public void onTick() {
        // Release jump key if Strategy 2 is active
        if (jumpKeyPressTicksRemaining > 0) {
            jumpKeyPressTicksRemaining--;
            if (jumpKeyPressTicksRemaining == 0) {
                MinecraftClient mc = MinecraftClient.getInstance();
                if (mc != null) mc.options.jumpKey.setPressed(false);
            }
        }

        // Damage detection: hurtTime resets to its max (10) on each new hit.
        // We fire when it transitions from any lower value back up to 10.
        // This runs every tick so it's always current — no mixin needed.
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc == null || mc.player == null) return;

        int hurtTime = mc.player.hurtTime;

        // hurtTime == 10 means a fresh hit just registered this tick
        if (hurtTime == 10 && prevHurtTime < 10) {
            System.out.println("[TriggerBot] hurtTime triggered: " + prevHurtTime + " -> " + hurtTime);
            onDamageTaken();
        }

        prevHurtTime = hurtTime;
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
        return getFacingDiff(mc, attacker) <= 90f;
    }
}
