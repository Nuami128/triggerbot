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

        // Only sprint if moving forward
        if (!mc.options.forwardKey.isPressed()) return;

        // Don't sprint if using item (eating/shielding)
        if (mc.player.isUsingItem()) return;

        // Don't sprint if in water or on ladder etc
        if (mc.player.isTouchingWater()) return;

        mc.player.setSprinting(true);
    }
}
