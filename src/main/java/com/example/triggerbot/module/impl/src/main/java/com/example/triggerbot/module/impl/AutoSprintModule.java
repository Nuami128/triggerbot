package com.example.triggerbot.module.impl;

import com.example.triggerbot.module.ClientModule;
import net.minecraft.client.MinecraftClient;

public class AutoSprintModule implements ClientModule {

    @Override
    public String getName() {
        return "AutoSprint";
    }

    @Override
    public void onEnable() {}

    @Override
    public void onDisable() {}

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
