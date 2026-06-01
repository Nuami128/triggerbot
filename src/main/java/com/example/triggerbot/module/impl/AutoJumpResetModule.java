package com.example.triggerbot.module.impl;

import com.example.triggerbot.module.EmptyModule;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;

public class AutoJumpResetModule extends EmptyModule {

    private boolean shouldJump = false;
    private int releaseTimer = 0;
    private double savedVelX = 0;
    private double savedVelZ = 0;
    private boolean restoreVelocity = false;

    public AutoJumpResetModule() {
        super("Auto Jump Reset");
    }

    @Override
    public void onClientTick() {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc == null || mc.player == null || mc.world == null) return;

        if (releaseTimer > 0) {
            releaseTimer--;
            if (releaseTimer == 0) {
                mc.options.jumpKey.setPressed(false);
            }
        }

        // Restore horizontal velocity after jump to keep KB at 100%
        if (restoreVelocity) {
            restoreVelocity = false;
            Vec3d current = mc.player.getVelocity();
            mc.player.setVelocity(savedVelX, current.y, savedVelZ);
        }

        if (!mc.player.isOnGround()) return;

        double x = mc.player.getX();
        double y = mc.player.getY();
        double z = mc.player.getZ();
        Box searchBox = new Box(x - 4, y - 2, z - 4, x + 4, y + 2, z + 4);

        for (Entity e : mc.world.getEntitiesByClass(LivingEntity.class, searchBox, entity ->
                entity != mc.player && entity.isAlive() && !entity.isSpectator())) {

            if (!(e instanceof PlayerEntity pe)) continue;

            if (pe.handSwingProgress > 0.0f && pe.handSwingProgress < 0.3f) {
                mc.options.jumpKey.setPressed(true);
                releaseTimer = 2;
                break;
            }
        }
    }

    @Override
    public void onDamage() {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc == null || mc.player == null) return;

        // Save horizontal velocity before jump so we can restore it
        Vec3d vel = mc.player.getVelocity();
        savedVelX = vel.x;
        savedVelZ = vel.z;
        restoreVelocity = true;
        shouldJump = true;
    }

    @Override
    public void onPostMovement() {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc == null || mc.player == null) return;
        if (!shouldJump) return;
        shouldJump = false;
        if (!mc.player.isOnGround()) return;
        mc.player.jump();
    }

    @Override
    public void onTick() {}

    @Override
    public void onJumpReset() {}
}
