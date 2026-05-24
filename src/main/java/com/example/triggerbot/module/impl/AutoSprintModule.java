package com.example.triggerbot.module.impl;

import com.example.triggerbot.module.EmptyModule;
import net.minecraft.client.MinecraftClient;

public class AutoSprintModule extends EmptyModule {

    public AutoSprintModule() {
        super("Auto Sprint");
    }

    @Override
    public void onTick() {
        MinecraftClient mc = MinecraftClient.getInstance();

        if (mc.player == null) return;
        if (mc.currentScreen != null) return;
        if (mc.player.isDead()) return;
        if (mc.player.isUsingItem()) return;
        if (mc.player.isTouchingWater()) return;
        if (!mc.options.forwardKey.isPressed()) return;

        if (!mc.player.isSprinting()) {
            mc.player.setSprinting(true);
        }
    }
}
