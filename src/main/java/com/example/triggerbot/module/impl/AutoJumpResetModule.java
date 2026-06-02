package com.example.triggerbot.module.impl;

import com.example.triggerbot.module.EmptyModule;
import net.minecraft.client.MinecraftClient;

public class AutoJumpResetModule extends EmptyModule {

    private int cooldown = 0;

    public AutoJumpResetModule() {
        super("Auto Jump Reset");
    }

    @Override
    public void onTick() {
        if (cooldown > 0) {
            cooldown--;
            return;
        }

        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc == null || mc.player == null) return;
        if (!isEnabled()) return;

        // Only jump reset when grounded and sprinting
        if (!mc.player.isOnGround() || !mc.player.isSprinting()) return;

        // Check if we just hit someone (lastAttackedTicks resets on hit)
        if (mc.player.getLastAttackedTicks() == 1) {
            mc.player.jump();
            cooldown = 10;
        }
    }
}
