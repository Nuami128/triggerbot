package com.example.triggerbot.module.impl;

import com.example.triggerbot.module.EmptyModule;
import net.minecraft.client.MinecraftClient;

public class AutoSprintModule extends EmptyModule {

    public AutoSprintModule() {
        super("AutoSprint");
    }

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

        mc.player.input.pressSprint = true;
    }

    @Override
    public void onPostMovement() {}
}
