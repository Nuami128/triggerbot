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
    }

    @Override
    public void onTick() {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc == null || mc.player == null) return;
        if (!isEnabled()) return;

        // Wait a few ticks after a hit before re-sprinting
        // This prevents NoSlow flags from sprinting too fast after attack
        if (attackBufferTicks > 0) {
            attackBufferTicks--;
            return;
        }

        // Only force sprint when moving forward and not already sprinting
        if (mc.options.forwardKey.isPressed() && !mc.player.isSprinting()) {
            mc.player.setSprinting(true);
        }
    }

    @Override
    public void onAttack() {
        // Buffer re-sprinting for 3 ticks after a hit
        attackBufferTicks = 3;
    }

    @Override public void onClientTick() {}
    @Override public void onJumpReset() {}
    @Override public void onPostMovement() {}
    @Override public void onDamage() {}
}
