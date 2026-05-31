package com.example.triggerbot.module.impl;

import com.example.triggerbot.module.EmptyModule;
import net.minecraft.client.MinecraftClient;

public class AutoJumpResetModule extends EmptyModule {

    private int cooldown = 0;

    public AutoJumpResetModule() {
        super("Auto Jump Reset");
    }

    @Override
    public void onTick() {
        if (cooldown > 0) cooldown--;
    }

    @Override
    public void onDamage() {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc == null || mc.player == null) return;
        if (cooldown > 0) return;

        mc.execute(() -> {
            if (mc.player == null) return;
            mc.player.jump();
            cooldown = 20;
        });
    }

    @Override
    public void onJumpReset() {}

    @Override
    public void onPostMovement() {}
}
