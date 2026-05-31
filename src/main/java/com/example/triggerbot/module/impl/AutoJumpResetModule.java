package com.example.triggerbot.module.impl;

import com.example.triggerbot.module.EmptyModule;
import net.minecraft.client.MinecraftClient;

public class AutoJumpResetModule extends EmptyModule {

    private boolean shouldJump = false;

    public AutoJumpResetModule() {
        super("Auto Jump Reset");
    }

    @Override
    public void onDamage() {
        shouldJump = true;
    }

    @Override
    public void onPostMovement() {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc == null || mc.player == null) return;
        if (!shouldJump) return;

        shouldJump = false;
        if (!mc.player.isOnGround()) return;

        mc.player.jump();
    }

    @Override
    public void onClientTick() {}

    @Override
    public void onTick() {}

    @Override
    public void onJumpReset() {}
}
