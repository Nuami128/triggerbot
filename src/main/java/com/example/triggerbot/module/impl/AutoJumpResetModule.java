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
        if (!isEnabled()) return;
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc == null || mc.player == null) return;

        if (cooldown > 0) cooldown--;

        int hurtTime = mc.player.hurtTime;

        // Trigger on the leading edge of hurtTime going UP (got hit)
        // hurtTime jumps to 10 the tick you're hit, then counts down
        if (hurtTime > lastHurtTime && hurtTime == 10 && cooldown == 0) {
            pendingJump = 1 + (int)(Math.random() * 2);
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
