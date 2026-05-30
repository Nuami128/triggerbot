package com.example.triggerbot.module.impl;

import com.example.triggerbot.module.EmptyModule;
import net.minecraft.client.MinecraftClient;

public class AutoJumpResetModule extends EmptyModule {

    private int lastHurtTime = 0;
    private int hurtLockTicks = 0;

    public AutoJumpResetModule() {
        super("Auto Jump Reset");
    }

    @Override
    public void onTick() {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc == null || mc.player == null) return;

        if (hurtLockTicks > 0) hurtLockTicks--;

        int hurt = mc.player.hurtTime;

        // Exact tick damage is received
        if (hurt > lastHurtTime && hurt >= 9 && hurtLockTicks == 0) {
            hurtLockTicks = 10;

            if (mc.player.isOnGround()) {
                mc.player.jump();
                System.out.println("[AutoJR] JUMP FIRED");
            }
        }

        lastHurtTime = hurt;
    }

    @Override
    public void onPostMovement() {}
}
