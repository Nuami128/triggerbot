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
        // Let the game handle sprint naturally when disabled
    }

    @Override
    public void onTick() {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc == null || mc.player == null) return;
        if (!isEnabled()) return;

        // Wait after a hit before re-sprinting (prevents NoSlow)
        if (attackBufferTicks > 0) {
            attackBufferTicks--;
            return;
        }

        boolean forwardHeld = mc.options.forwardKey.isPressed();
        boolean alreadySprinting = mc.player.isSprinting();

        // KEY FIX: Only call setSprinting when state actually needs to change.
        // Calling it every tick causes a continuous Grim simulation desync.
        if (forwardHeld && !alreadySprinting) {
            mc.player.setSprinting(true);
        }
        // Do NOT force setSprinting(false) here — let Minecraft handle it naturally
        // when the player stops moving or gets hit.
    }

    @Override
    public void onAttack() {
        attackBufferTicks = 3;
    }

    @Override public void onClientTick() {}
    @Override public void onJumpReset() {}
    @Override public void onPostMovement() {}
    @Override public void onDamage() {}
}
