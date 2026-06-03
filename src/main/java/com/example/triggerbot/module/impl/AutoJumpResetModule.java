package com.example.triggerbot.module.impl;

import com.example.triggerbot.module.EmptyModule;
import net.minecraft.client.MinecraftClient;

public class AutoJumpResetModule extends EmptyModule {

    private int cooldown = 0;
    private int pendingJump = -1;
    private int lastHurtTime = 0;

    public AutoJumpResetModule() {
        super("Auto Jump Reset");
    }

    @Override
    public void onTick() {
        if (cooldown > 0) cooldown--;

        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc == null || mc.player == null) return;
        if (!isEnabled()) return;

        int hurtTime = mc.player.hurtTime;
        if (hurtTime == 9 && lastHurtTime != 9 && cooldown == 0) {
            pendingJump = 1 + (int)(Math.random() * 3);
        }
        lastHurtTime = hurtTime;

        if (pendingJump > 0) {
            pendingJump--;
            if (pendingJump == 0) {
                if (mc.player.isOnGround()) {
                    mc.player.input.jump();
                    cooldown = 10;
                }
                pendingJump = -1;
            }
        }
    }

    @Override public void onAttack() {}
    @Override public void onClientTick() {}
    @Override public void onJumpReset() {}
    @Override public void onPostMovement() {}
    @Override public void onDamage() {}
}
