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

    @Override public void onTick() {}
    @Override public void onClientTick() {}
    @Override public void onAttack() {}
    @Override public void onJumpReset() {}
    @Override public void onDamage() {}

    @Override
    public void onPostMovement() {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc == null || mc.player == null) return;
        if (!isEnabled()) return;

        if (releaseJump) {
            mc.options.jumpKey.setPressed(false);
            releaseJump = false;
        }

        int hurtTime = mc.player.hurtTime;

        // Use a broad transition check instead of strict 10→9.
        // On Android/high-ping connections, server packets arrive
        // asynchronously so hurtTime can skip values between ticks.
        // Catching any drop from >=10 to <10 ensures we never miss a hit.
        if (lastHurtTime >= 10 && hurtTime < 10 && hurtTime > 0) {
            mc.options.jumpKey.setPressed(true);
            releaseJump = true;
        }

        lastHurtTime = hurtTime;
    }
}
