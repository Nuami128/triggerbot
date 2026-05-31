package com.example.triggerbot.module.impl;

import com.example.triggerbot.module.EmptyModule;
import net.minecraft.client.MinecraftClient;

public class AutoJumpResetModule extends EmptyModule {

    private boolean shouldJump = false;
    private boolean hasJumped = false;
    private int cooldown = 0;
    private int groundedTicks = 0;

    public AutoJumpResetModule() {
        super("Auto Jump Reset");
    }

    @Override
    public void onTick() {
        if (cooldown > 0) cooldown--;

        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc == null || mc.player == null) return;

        if (mc.player.isOnGround()) {
            groundedTicks++;
            if (hasJumped) hasJumped = false;
        } else {
            groundedTicks = 0;
        }
    }

    @Override
    public void onJumpReset() {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc == null || mc.player == null) return;
        if (mc.currentScreen != null) return;
        if (mc.player.isUsingItem()) return;

        int hurtTime = mc.player.hurtTime;

        // Only queue jump if we were recently on the ground and haven't jumped yet
        if (hurtTime == 9 && !shouldJump && cooldown == 0 && !hasJumped && groundedTicks > 0) {
            shouldJump = true;
        }

        if (shouldJump && !hasJumped) {
            mc.player.jump();
            shouldJump = false;
            hasJumped = true;
            cooldown = 15;
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
