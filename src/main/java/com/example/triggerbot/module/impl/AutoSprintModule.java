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

        if (attackBufferTicks > 0) {
            attackBufferTicks--;
            return;
        }

        // Only set sprint when W is held and not already sprinting
        if (mc.options.forwardKey.isPressed() && !mc.player.isSprinting()) {
            mc.player.setSprinting(true);
        }
    }

    // Called by TriggerBotModule when a hit lands
    public void notifyHit() {
        // 10 ticks = 0.5s buffer — enough for Minecraft's sprint-break to fully resolve
        attackBufferTicks = 10;
    }

    @Override public void onAttack() {}
    @Override public void onClientTick() {}
    @Override public void onJumpReset() {}
    @Override public void onPostMovement() {}
    @Override public void onDamage() {}
}
