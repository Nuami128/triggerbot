package com.example.triggerbot.module.impl;

import com.example.triggerbot.module.EmptyModule;
import net.minecraft.client.MinecraftClient;

public class AutoJumpResetModule extends EmptyModule {

    private int lastHurtTime = 0;
    private boolean shouldJump = false;
    private int releaseTimer = 0;

    public AutoJumpResetModule() {
        super("Auto Jump Reset");
    }

    @Override
    public void onTick() {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc == null || mc.player == null) return;

        // Automatically unpress the key next tick so the spacebar isn't stuck "down"
        if (releaseTimer > 0) {
            releaseTimer--;
            if (releaseTimer == 0) {
                mc.options.jumpKey.setPressed(false);
            }
        }

        int hurtTime = mc.player.hurtTime;

        // Rising edge check
        if (hurtTime > lastHurtTime && hurtTime > 0) {
            if (mc.player.isOnGround()) {
                shouldJump = true;
            }
        }

        if (hurtTime == 0 || !mc.player.isOnGround()) {
            shouldJump = false;
        }

        lastHurtTime = hurtTime;
    }

    @Override
    public void onJumpReset() {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc == null || mc.player == null) return;
        if (mc.currentScreen != null || mc.player.isUsingItem()) return;

        if (shouldJump && mc.player.isOnGround()) {
            // If Claude's suggestion of mc.player.jump() still flags Ground Spoof/Simulation on your server,
            // use this hardware key spoofing block instead. It compiles cleanly on 1.21.11 Yarn:
            mc.options.jumpKey.setPressed(true);
            releaseTimer = 1; 
            
            shouldJump = false;
            System.out.println("[AutoJR] Safe Key-Spoofed Jump Reset Fired");
        }
    }

    @Override
    public void onPostMovement() {
        // Keeps running safely at tick HEAD via your mixin setup
    }
}
