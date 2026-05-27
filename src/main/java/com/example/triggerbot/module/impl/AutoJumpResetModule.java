package com.example.triggerbot.module.impl;

import com.example.triggerbot.module.EmptyModule;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.math.MathHelper;

public class AutoJumpResetModule extends EmptyModule {

    private int prevHurtTime = 0;
    private int jumpKeyPressTicksRemaining = 0;

    // Track the last few hurtTime values so we can see the pattern in chat
    private int diagTick = 0;

    public AutoJumpResetModule() {
        super("Auto Jump Reset");
    }

    public void onDamageTaken() {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player == null || mc.world == null) return;

        // Log every check so we know exactly where it's stopping
        if (mc.player.isUsingItem()) { debug(mc, "SKIP: eating"); return; }

        boolean onGround = mc.player.isOnGround();
        double velY = mc.player.getVelocity().y;
        boolean sprinting = mc.player.isSprinting();
        double velX = mc.player.getVelocity().x;
        double velZ = mc.player.getVelocity().z;
        double horizSpeedSq = velX * velX + velZ * velZ;

        debug(mc, "onGround=" + onGround + " velY=" + String.format("%.2f", velY)
                + " sprint=" + sprinting + " hSpd=" + String.format("%.3f", horizSpeedSq));

        if (!onGround) { debug(mc, "SKIP: airborne"); return; }
        if (velY > 0) { debug(mc, "SKIP: ascending"); return; }
        if (!sprinting) { debug(mc, "SKIP: not sprinting"); return; }
        if (horizSpeedSq < 0.001) { debug(mc, "SKIP: not moving"); return; }

        Entity attacker = findAttacker(mc);
        if (attacker == null) { debug(mc, "SKIP: no attacker"); return; }
        float angleDiff = getFacingDiff(mc, attacker);
        if (angleDiff > 90f) { debug(mc, "SKIP: angle " + (int) angleDiff + "deg"); return; }

        mc.player.jump();

        // Also try key simulation as backup — comment out jump() above if this works better
        // jumpKeyPressTicksRemaining = 2;
        // mc.options.jumpKey.setPressed(true);

        debug(mc, "FIRED!");
        System.out.println("[TriggerBot] jump fired");
    }

    @Override
    public void onTick() {
        if (jumpKeyPressTicksRemaining > 0) {
            jumpKeyPressTicksRemaining--;
            if (jumpKeyPressTicksRemaining == 0) {
                MinecraftClient mc = MinecraftClient.getInstance();
                if (mc != null) mc.options.jumpKey.setPressed(false);
            }
        }

        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc == null || mc.player == null) { prevHurtTime = 0; return; }

        int hurtTime = mc.player.hurtTime;

        // Log every hurtTime change so we can see the actual values
        if (hurtTime != prevHurtTime) {
            System.out.println("[TriggerBot] hurtTime: " + prevHurtTime + " -> " + hurtTime);
        }

        // Fire when hurtTime rises — covers any max value (10, 20, or other)
        // prevHurtTime <= 1 means we were at the tail end or idle,
        // and now hurtTime jumped up, meaning a new hit just landed.
        if (hurtTime > prevHurtTime && prevHurtTime <= 1) {
            System.out.println("[TriggerBot] Hit detected! hurtTime=" + hurtTime);
            // Show in chat too so you can see it on device without logcat
            mc.player.sendMessage(Text.literal("JR: hit detected! ht=" + hurtTime), true);
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

    private boolean isAttackerInFront(MinecraftClient mc, Entity attacker) {
        return getFacingDiff(mc, attacker) <= 90f;
    }
}
