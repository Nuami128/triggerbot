package com.example.triggerbot.module.impl;

import com.example.triggerbot.module.EmptyModule;
import net.minecraft.client.MinecraftClient;

public class AutoJumpResetModule extends EmptyModule {

    private int lastHurtTime = 0;
    private int jumpTicks = 0;

    public AutoJumpResetModule() {
        super("Auto Jump Reset");
    }

    @Override
    public void onJumpReset() {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc == null || mc.player == null) return;
        if (mc.currentScreen != null || mc.player.isUsingItem()) return;

        int currentHurtTime = mc.player.hurtTime;

        // 1. Your Rising Edge Interceptor: Catches the exact millisecond damage transitions from 0
        if (currentHurtTime > lastHurtTime) {
    jumpTicks = 2;
        }

        // 2. The Step-Down Key Injection Matrix
        if (jumpTicks > 0) {
            // Simulate a genuine, un-erasable vanilla Spacebar press
            mc.options.jumpKey.setPressed(true); 
            
            jumpTicks--;
        } else {
            // Automatically clear the key state once the execution window concludes
            mc.options.jumpKey.setPressed(false);
        }

        lastHurtTime = currentHurtTime;
    }

    @Override
    public void onTick() {}

    @Override
    public void onPostMovement() {}
}
