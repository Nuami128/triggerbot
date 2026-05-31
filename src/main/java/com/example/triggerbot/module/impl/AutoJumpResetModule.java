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

    public AutoJumpResetModule() {
        super("Auto Jump Reset");
    }

    @Override
    public void onClientTick() {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc == null || mc.player == null || mc.world == null) return;

        // Release jump key after 2 ticks
        if (releaseTimer > 0) {
            releaseTimer--;
            if (releaseTimer == 0) {
                mc.options.jumpKey.setPressed(false);
            }
        }

        // Pre-jump when nearby enemy is swinging at us
        if (!mc.player.isOnGround()) return;

        Vec3d pos = mc.player.getPos();
        Box searchBox = new Box(pos.x - 4, pos.y - 2, pos.z - 4,
                                pos.x + 4, pos.y + 2, pos.z + 4);

        for (Entity e : mc.world.getEntitiesByClass(LivingEntity.class, searchBox, entity ->
                entity != mc.player && entity.isAlive() && !entity.isSpectator())) {

            if (!(e instanceof PlayerEntity pe)) continue;

            // Detect swing: handSwingProgress > 0 means they just started swinging
            if (pe.handSwingProgress > 0.0f && pe.handSwingProgress < 0.3f) {
                mc.options.jumpKey.setPressed(true);
                releaseTimer = 2;
                break;
            }
        }
    }

    @Override
    public void onDamage() {
        // Fallback — jump if we somehow missed the swing detection
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
