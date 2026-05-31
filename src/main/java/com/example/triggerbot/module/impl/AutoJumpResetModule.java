package com.example.triggerbot.module.impl;

import com.example.triggerbot.module.EmptyModule;
import net.minecraft.client.MinecraftClient;

public class AutoJumpResetModule extends EmptyModule {

    public AutoJumpResetModule() {
        super("Auto Jump Reset");
    }

    @Override
    public void onDamage() {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc == null || mc.player == null) return;

        mc.execute(() -> {
            if (mc.player == null) return;
            if (!mc.player.isOnGround()) return;
            mc.player.jump();
        });
    }

    @Override
    public void onTick() {}

    @Override
    public void onClientTick() {}

    @Override
    public void onPostMovement() {}

    @Override
    public void onJumpReset() {}
}
