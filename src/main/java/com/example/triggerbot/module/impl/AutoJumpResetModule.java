package com.example.triggerbot.module.impl;

import com.example.triggerbot.module.EmptyModule;
import net.minecraft.client.MinecraftClient;

public class AutoJumpResetModule extends EmptyModule {

    private boolean releaseJump = false;
    private int lastHurtTime = 0;

    public AutoJumpResetModule() {
        super("Auto Jump Reset");
    }

    @Override
    public void onEnable() {
        super.onEnable();
        releaseJump = false;
        lastHurtTime = 0;
    }

    @Override
    public void onDisable() {
        super.onDisable();
        releaseJump = false;
        lastHurtTime = 0;
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc != null && mc.options != null) mc.options.jumpKey.setPressed(false);
    }

    @Override
    public void onTick() {}

    @Override
    public void onClientTick() {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc == null || mc.player == null) return;
        if (!isEnabled()) return;

        // Release the jump key on the tick after we pressed it
        if (releaseJump) {
            mc.options.jumpKey.setPressed(false);
            releaseJump = false;
        }

        int hurtTime = mc.player.hurtTime;

        // hurtTime starts at 10 when hit and counts down each tick.
        // We detect the transition from any higher value down to 9,
        // which is exactly 1 tick after the server confirms the hit.
        // This is reliable, purely client-side, needs no mixin.
        if (hurtTime == 9 && lastHurtTime == 10) {
            mc.options.jumpKey.setPressed(true);
            releaseJump = true;
        }

        lastHurtTime = hurtTime;
    }

    @Override public void onAttack() {}
    @Override public void onJumpReset() {}
    @Override public void onPostMovement() {}
    @Override public void onDamage() {}
}
