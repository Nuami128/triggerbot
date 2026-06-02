package com.example.triggerbot.module.impl;

import com.example.triggerbot.module.EmptyModule;
import net.minecraft.client.MinecraftClient;

public class AutoJumpResetModule extends EmptyModule {

    private int cooldown = 0;
    private int lastHurtTime = 0;

    public AutoJumpResetModule() {
        super("Auto Jump Reset");
    }

    @Override
    public void onTick() {
        if (cooldown > 0) {
            cooldown--;
            return;
        }

        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc == null || mc.player == null) return;
        if (!isEnabled()) return;
        if (!mc.player.isOnGround() || !mc.player.isSprinting()) return;

        int hurtTime = mc.player.hurtTime;

        if (hurtTime != lastHurtTime && hurtTime == 0 && lastHurtTime == 1) {
            mc.player.jump();
            cooldown = 10;
        }

        lastHurtTime = hurtTime;
    }

    @Override
    public void onClientTick() {}

    @Override
    public void onJumpReset() {}

    @Override
    public void onPostMovement() {}

    @Override
    public void onDamage() {}
}
