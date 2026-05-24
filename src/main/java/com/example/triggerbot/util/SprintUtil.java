package com.example.triggerbot.util;

import net.minecraft.client.MinecraftClient;

public class SprintUtil {

    public static void tick(MinecraftClient mc) {
        if (mc.player == null) return;
        if (mc.currentScreen != null) return;
        if (mc.player.isDead()) return;
        if (mc.player.isUsingItem()) return;
        if (mc.player.isTouchingWater()) return;
        if (!mc.options.forwardKey.isPressed()) return;

        // Force sprint every tick regardless of what cancelled it
        mc.player.setSprinting(true);
        mc.options.sprintKey.setPressed(true);
    }
}
