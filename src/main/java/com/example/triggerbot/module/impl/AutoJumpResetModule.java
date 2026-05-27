package com.example.triggerbot.module.impl;

import com.example.triggerbot.module.EmptyModule;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.math.MathHelper;

public class AutoJumpResetModule extends EmptyModule {

    // How many ticks to hold the jump key pressed after triggering.
    // 1 tick is enough to register a keydown event. 2 is safer.
    private int jumpKeyPressTicksRemaining = 0;

    public AutoJumpResetModule() {
        super("Auto Jump Reset");
    }

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

        // Strategy 1: directly call jump() — works for vanilla jump counter
        mc.player.jump();

        // Strategy 2: simulate key press — needed if the counter mod watches
        // for actual key events rather than Y-velocity changes.
        // This fires jumpKey.setPressed(true) for 2 ticks, then releases in onTick().
        // Uncomment if jump() alone doesn't register with the counter mod.
        //
        // jumpKeyPressTicksRemaining = 2;
        // mc.options.jumpKey.setPressed(true);

        debug(mc, "Fired!");
        System.out.println("[TriggerBot] AutoJumpReset: jump() called");
    }

    @Override
    public void onTick() {
        // Handle key-press release for Strategy 2.
        // Safe to keep active even if Strategy 1 is used — does nothing when counter is 0.
        if (jumpKeyPressTicksRemaining > 0) {
            jumpKeyPressTicksRemaining--;
            if (jumpKeyPressTicksRemaining == 0) {
                MinecraftClient mc = MinecraftClient.getInstance();
                if (mc != null) {
                    mc.options.jumpKey.setPressed(false);
                    System.out.println("[TriggerBot] AutoJumpReset: jumpKey released");
                }
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
