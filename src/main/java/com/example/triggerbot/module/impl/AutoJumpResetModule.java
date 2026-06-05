package com.example.triggerbot.module.impl;

import com.example.triggerbot.module.EmptyModule;
import net.minecraft.client.MinecraftClient;

public class AutoJumpResetModule extends EmptyModule {

    private int cooldown = 0;
    private int pendingJump = -1;
    private int lastHurtTime = 0;
    private boolean releaseJump = false;

    public AutoJumpResetModule() {
        super("Auto Jump Reset");
    }

    @Override
    public void onEnable() {
        super.onEnable();
        cooldown = 0;
        pendingJump = -1;
        lastHurtTime = 0;
        releaseJump = false;
    }

    @Override
    public void onDisable() {
        super.onDisable();
        cooldown = 0;
        pendingJump = -1;
        lastHurtTime = 0;
        releaseJump = false;
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc != null) mc.options.jumpKey.setPressed(false);
    }

    @Override
    public void onTick() {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc == null || mc.player == null) return;
        if (!isEnabled()) return;

        // Release the jump key on the tick after we pressed it (tap behaviour)
        if (releaseJump) {
            mc.options.jumpKey.setPressed(false);
            releaseJump = false;
        }

        if (cooldown > 0) cooldown--;

        // Detect when WE get hit — hurtTime resets to 10 on damage received
        int hurtTime = mc.player.hurtTime;
        if (hurtTime == 10 && lastHurtTime != 10 && cooldown == 0) {
            pendingJump = 1 + (int)(Math.random() * 2); // 1-2 tick delay
        }
        lastHurtTime = hurtTime;

        // Execute pending jump
        if (pendingJump > 0) {
            pendingJump--;
            if (pendingJump == 0) {
                if (mc.player.isOnGround()) {
                    // Press jump key for exactly one tick (tap)
                    mc.options.jumpKey.setPressed(true);
                    releaseJump = true; // released next tick
                    cooldown = 8;
                }
                pendingJump = -1;
            }
        }
    }

    @Override public void onAttack() {} // intentionally empty
    @Override public void onClientTick() {}
    @Override public void onJumpReset() {}
    @Override public void onPostMovement() {}
    @Override public void onDamage() {}
}
