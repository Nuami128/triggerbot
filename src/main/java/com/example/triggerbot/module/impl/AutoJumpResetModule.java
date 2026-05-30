package com.example.triggerbot.module.impl;

import com.example.triggerbot.module.EmptyModule;
import net.minecraft.client.MinecraftClient;

public class AutoJumpResetModule extends EmptyModule {

    public AutoJumpResetModule() {
        super("Auto Jump Reset");
    }

    @Override
    public void onTick() {}

    @Override
    public void onJumpReset() {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc == null || mc.player == null) return;
        if (mc.currentScreen != null) return;
        if (mc.player.isUsingItem()) return;
        if (mc.player.hurtTime == 0) return;
        if (mc.player.hurtTime == mc.player.maxHurtTime) return;
        if (!mc.player.isOnGround()) return;

        if (mc.player.hurtTime == 9) {
            mc.player.jump();
            System.out.println("[AutoJR] JUMP FIRED");
        }
    }

    @Override
    public void onPostMovement() {}

    @Override
    public void onDamage() {}
}
