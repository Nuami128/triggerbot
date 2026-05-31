package com.example.triggerbot.module.impl;

import com.example.triggerbot.module.EmptyModule;
import net.minecraft.client.MinecraftClient;

public class AutoJumpResetModule extends EmptyModule {

    private boolean shouldJump = false;

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

        int hurtTime = mc.player.hurtTime;

        // hurtTime starts at 9 in this version, not 10
        if (hurtTime == 9 && !shouldJump) {
            shouldJump = true;
        }

        if (shouldJump && mc.player.isOnGround()) {
            mc.player.jump();
            shouldJump = false;
            System.out.println("[AutoJR] JUMP FIRED");
        }

        if (hurtTime == 0) {
            shouldJump = false;
        }
    }

    @Override
    public void onPostMovement() {}

    @Override
    public void onDamage() {}
}
