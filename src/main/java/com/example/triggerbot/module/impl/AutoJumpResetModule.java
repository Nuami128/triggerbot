package com.example.triggerbot.module.impl;

import com.example.triggerbot.module.EmptyModule;
import net.minecraft.client.MinecraftClient;

public class AutoJumpResetModule extends EmptyModule {

    private int cooldown = 0;
    private int pendingJump = -1;

    public AutoJumpResetModule() {
        super("Auto Jump Reset");
    }

    @Override
    public void onTick() {
        if (cooldown > 0) cooldown--;

        if (pendingJump > 0) {
            pendingJump--;
            if (pendingJump == 0) {
                MinecraftClient mc = MinecraftClient.getInstance();
                if (mc != null && mc.player != null && mc.player.isOnGround()) {
                    mc.player.input.jump();
                    cooldown = 10;
                }
                pendingJump = -1;
            }
        }
    }

    @Override
    public void onAttack() {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc == null || mc.player == null) return;
        if (!isEnabled()) return;
        if (!mc.player.isOnGround()) return;
        if (mc.targetedEntity == null) return;
        if (cooldown > 0) return;

        pendingJump = 1 + (int)(Math.random() * 3);
    }

    @Override public void onClientTick() {}
    @Override public void onJumpReset() {}
    @Override public void onPostMovement() {}
    @Override public void onDamage() {}
}
