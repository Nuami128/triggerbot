package com.example.triggerbot.module.impl;

import com.example.triggerbot.module.EmptyModule;
import net.minecraft.client.MinecraftClient;

public class AutoJumpResetModule extends EmptyModule {

    private boolean hasJumped = false;
    private int cooldown = 0;
    private int lastHurtTime = 0;

    public AutoJumpResetModule() {
        super("Auto Jump Reset");
    }

    @Override
    public void onTick() {
        if (cooldown > 0) cooldown--;

        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc == null || mc.player == null) return;

        if (mc.player.isOnGround()) {
            if (hasJumped) hasJumped = false;
        }

        int hurtTime = mc.player.hurtTime;

        if (hurtTime == 9 && lastHurtTime != 9 && cooldown == 0 && !hasJumped) {
            mc.player.jump();
            hasJumped = true;
            cooldown = 15;
            System.out.println("[AutoJR] JUMP FIRED");
        }

        if (hurtTime == 0) hasJumped = false;
        lastHurtTime = hurtTime;
    }

    @Override
    public void onJumpReset() {}

    @Override
    public void onPostMovement() {}

    @Override
    public void onDamage() {}
}
