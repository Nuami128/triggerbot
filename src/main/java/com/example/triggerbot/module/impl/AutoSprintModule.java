package com.example.triggerbot.module.impl;

import com.example.triggerbot.module.EmptyModule;
import net.minecraft.client.MinecraftClient;

public class AutoSprintModule extends EmptyModule {

    private int attackBufferTicks = 0;

    public AutoSprintModule() {
        super("Auto Sprint");
    }

    @Override
    public void onEnable() {
        super.onEnable();
        attackBufferTicks = 0;
    }

    @Override
    public void onDisable() {
        super.onDisable();
        attackBufferTicks = 0;
        // Don't leave sprint stuck on
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc != null && mc.player != null) {
            mc.player.setSprinting(false);
        }
    }

    @Override
    public void onTick() {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc == null || mc.player == null) return;
        if (!isEnabled()) return;

        // After a hit, wait for Minecraft's natural sprint-break to finish
        // before we start forcing sprint again. This prevents NoSlow flags.
        if (attackBufferTicks > 0) {
            attackBufferTicks--;
            return;
        }

        // Only re-sprint when: moving forward, not already sprinting,
        // not using an item, and not in a screen.
        if (mc.options.forwardKey.isPressed()
                && !mc.player.isSprinting()
                && !mc.player.isUsingItem()
                && mc.currentScreen == null) {
            mc.player.setSprinting(true);
        }
    }

    @Override
    public void onAttack() {
        // After landing a hit, let Minecraft's sprint-break happen naturally.
        // 5 ticks gives Grim time to see the natural sprint interruption
        // before we start re-sprinting. This fixes NoSlow flags.
        attackBufferTicks = 5;
    }

    @Override public void onClientTick() {}
    @Override public void onJumpReset() {}
    @Override public void onPostMovement() {}
    @Override public void onDamage() {}
}
