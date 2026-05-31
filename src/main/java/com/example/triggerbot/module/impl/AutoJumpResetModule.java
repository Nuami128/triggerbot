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
        if (!mc.player.isOnGround()) return;

        mc.execute(() -> {
            if (mc.player == null) return;
            mc.options.jumpKey.setPressed(true);
            cooldown = 2;
        });
    }

    @Override
    public void onPostMovement() {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc == null || mc.player == null) return;
        if (cooldown <= 1) {
            mc.options.jumpKey.setPressed(false);
        }
    }

    @Override
    public void onJumpReset() {}
}
