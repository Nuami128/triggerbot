package com.example.triggerbot.module.impl;

import com.example.triggerbot.module.EmptyModule;
import net.minecraft.client.MinecraftClient;

public class AutoJumpResetModule extends EmptyModule {

    private int jumpDelay = -1;

    public AutoJumpResetModule() {
        super("Auto Jump Reset");
    }

    @Override
    public void onDamage() {
        if (jumpDelay < 0) jumpDelay = 2;
    }

    @Override
    public void onClientTick() {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc == null || mc.player == null) return;

        if (jumpDelay > 0) {
            jumpDelay--;
            return;
        }

        if (jumpDelay == 0) {
            jumpDelay = -1;
            if (!mc.player.isOnGround()) return;
            mc.options.jumpKey.setPressed(true);
        } else {
            mc.options.jumpKey.setPressed(false);
        }
    }

    @Override
    public void onTick() {}

    @Override
    public void onPostMovement() {}

    @Override
    public void onJumpReset() {}
}
