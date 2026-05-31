package com.example.triggerbot.module.impl;

import com.example.triggerbot.module.EmptyModule;
import net.minecraft.client.MinecraftClient;

public class AutoJumpResetModule extends EmptyModule {

    public AutoJumpResetModule() {
        super("Auto Jump Reset");
    }

    @Override
    public void onClientTick() {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc == null || mc.player == null) return;
        if (mc.currentScreen != null) return;
        if (mc.player.isUsingItem()) return;
        if (mc.player.hurtTime == 0) return;
        if (mc.player.hurtTime == mc.player.maxHurtTime) return;
        if (mc.player.hurtTime != 9) return;
        if (!mc.player.isOnGround()) return;

        mc.player.jump();
    }

    @Override
    public void onDamage() {}

    @Override
    public void onTick() {}

    @Override
    public void onPostMovement() {}

    @Override
    public void onJumpReset() {}
}
