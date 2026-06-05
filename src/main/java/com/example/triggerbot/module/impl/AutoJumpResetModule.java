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
    public void onEnable() {
        super.onEnable();
        cooldown = 0;
        pendingJump = -1;
        lastHurtTime = 0;
    }

    @Override
    public void onDisable() {
        super.onDisable();
        cooldown = 0;
        pendingJump = -1;
        lastHurtTime = 0;
    }

    // Run from clientTickAll (END_CLIENT_TICK) — hurtTime is fully updated here
    @Override
    public void onClientTick() {
        if (!isEnabled()) return;
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc == null || mc.player == null) return;

        if (cooldown > 0) cooldown--;

        int hurtTime = mc.player.hurtTime;

        // hurtTime jumps UP to 10 the exact tick a hit registers
        if (hurtTime == 10 && lastHurtTime < 10 && cooldown == 0) {
            pendingJump = 1; // fire next client tick
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

    @Override public void onTick() {}
    @Override public void onAttack() {}
    @Override public void onJumpReset() {}
    @Override public void onPostMovement() {}
    @Override public void onDamage() {}
}
