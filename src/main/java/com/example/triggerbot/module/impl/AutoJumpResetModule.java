package com.example.triggerbot.module.impl;

import com.example.triggerbot.module.EmptyModule;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.player.PlayerEntity;

public class AutoJumpResetModule extends EmptyModule {

    public AutoJumpResetModule() {
        super("Auto Jump Reset");
    }

    @Override
    public void onTick() {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc == null || mc.player == null) return;

        PlayerEntity player = mc.player;

        // Jump reset every tick if on ground and moving
        if (player.isOnGround() && isMoving(player)) {
            mc.execute(player::jump);
        }
    }

    @Override
    public void onDamage() {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc == null || mc.player == null) return;

        // Immediately jump reset on any damage event
        mc.execute(() -> {
            if (mc.player == null) return;
            if (mc.player.isOnGround()) {
                mc.player.jump();
            }
        });
    }

    private boolean isMoving(net.minecraft.entity.player.PlayerEntity player) {
        return player.getVelocity().horizontalLength() > 0.003;
    }

    @Override
    public void onJumpReset() {}

    @Override
    public void onPostMovement() {}
}
