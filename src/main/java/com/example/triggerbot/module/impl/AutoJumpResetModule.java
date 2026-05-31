package com.example.triggerbot.module.impl;

import com.example.triggerbot.module.EmptyModule;
import net.minecraft.client.MinecraftClient;

public class AutoJumpResetModule extends EmptyModule {

    private int holdTicks = 0;

    public AutoJumpResetModule() {
        super("Auto Jump Reset");
    }

    @Override
    public void onDamage() {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc == null || mc.player == null) return;
        if (!mc.player.isOnGround()) return;

        mc.execute(() -> {
            if (mc.player == null) return;
            mc.player.jump();
            holdTicks = 3;
        });
    }

    @Override
    public void onClientTick() {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc == null || mc.player == null) return;

        if (holdTicks > 0) {
            holdTicks--;
        }
    }

    @Override
    public void onTick() {}

    @Override
    public void onPostMovement() {}

    @Override
    public void onJumpReset() {}
}
