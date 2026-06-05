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
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc != null) mc.options.sprintKey.setPressed(false);
    }

    @Override
    public void onTick() {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc == null || mc.player == null) return;
        if (!isEnabled()) return;

        if (attackBufferTicks > 0) {
            // During buffer: release sprint key and let Minecraft's
            // natural sprint-break happen. Do NOT touch setSprinting.
            mc.options.sprintKey.setPressed(false);
            attackBufferTicks--;
            return;
        }

        // Mirror what the player is doing with forward key.
        // sprintKey follows forwardKey — Grim sees natural sprint behaviour.
        boolean movingForward = mc.options.forwardKey.isPressed();
        mc.options.sprintKey.setPressed(movingForward && !mc.player.isUsingItem());
    }

    @Override
    public void onAttack() {
        // Release sprint for 5 ticks after a hit so Grim sees
        // the natural sprint-break before we resume.
        attackBufferTicks = 5;
    }

    @Override public void onClientTick() {}
    @Override public void onJumpReset() {}
    @Override public void onPostMovement() {}
    @Override public void onDamage() {}
}
