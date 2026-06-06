package com.example.triggerbot.module.impl;

import net.minecraft.entity.LivingEntity;
import com.example.triggerbot.module.EmptyModule;
import net.minecraft.client.MinecraftClient;

public class AutoJumpResetModule extends EmptyModule {

    private boolean releaseJump = false;
    private boolean wasOnGround = false;

    public AutoJumpResetModule() {
        super("Auto Jump Reset");
    }

    @Override
    public void onEnable() {
        super.onEnable();
        releaseJump = false;
        wasOnGround = false;
    }

    @Override
    public void onDisable() {
        super.onDisable();
        releaseJump = false;
        wasOnGround = false;
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc != null) mc.options.jumpKey.setPressed(false);
    }

    // onTick fires BEFORE sendMovementPackets — too early for a jump.
    // Grim sees the jump velocity before the movement packet, flags Simulation.
    @Override
    public void onTick() {}

    @Override
    public void onClientTick() {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc == null || mc.player == null) return;
        if (!isEnabled()) return;

        if (releaseJump) {
            mc.options.jumpKey.setPressed(false);
            releaseJump = false;
        }

        boolean onGround = mc.player.isOnGround();

        // Only fire when:
        // 1. Just landed (onGround && !wasOnGround)
        // 2. Have a living target nearby
        // 3. Actually moving (sprinting or meaningful horizontal velocity)
        // 4. Target's hurtTime == 9 — fires the jump reset on the exact tick
        //    the server confirms the hit landed, so it syncs with knockback
        //    and won't look like an unprompted jump to Grim
        boolean hasTarget = mc.targetedEntity instanceof LivingEntity living
        && living.isAlive();
boolean hasMovement = mc.player.isSprinting()
        || (mc.player.getVelocity().horizontalLengthSquared() > 0.001);
boolean hurtTimeSynced = mc.targetedEntity instanceof LivingEntity le
        && le.hurtTime == 9;

        if (onGround && !wasOnGround && hasTarget && hasMovement && hurtTimeSynced) {
            mc.options.jumpKey.setPressed(true);
            releaseJump = true;
        }

        wasOnGround = onGround;
    }

    @Override public void onAttack() {}
    @Override public void onJumpReset() {}
    @Override public void onPostMovement() {}
    @Override public void onDamage() {}
}
