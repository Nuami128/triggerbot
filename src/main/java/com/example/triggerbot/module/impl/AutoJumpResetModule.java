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

    @Override
    public void onTick() {
        if (cooldown > 0) cooldown--;

        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc == null || mc.player == null) return;
        if (!isEnabled()) return;

        // Detect when WE get hit (hurtTime goes to 10 on damage)
        int hurtTime = mc.player.hurtTime;
        if (hurtTime == 10 && lastHurtTime != 10 && cooldown == 0) {
            // Queue a jump in 1-2 ticks so we're grounded when it fires
            pendingJump = 1 + (int)(Math.random() * 2);
        }
        lastHurtTime = hurtTime;

        // Process pending jump
        if (pendingJump > 0) {
            pendingJump--;
            if (pendingJump == 0) {
                if (mc.player.isOnGround()) {
                    mc.player.input.jump();
                    cooldown = 8;
                }
                pendingJump = -1;
            }
        }
    }

    // IMPORTANT: Do NOT implement onAttack for jump reset.
    // The PlayerAttackMixin calls onAttackAll() which was causing
    // the jump to fire on every triggerbot hit — that's what caused
    // the Simulation flags. Jump reset only fires when YOU get hit.
    @Override public void onAttack() {}

    @Override public void onClientTick() {}
    @Override public void onJumpReset() {}
    @Override public void onPostMovement() {}
    @Override public void onDamage() {}
}
