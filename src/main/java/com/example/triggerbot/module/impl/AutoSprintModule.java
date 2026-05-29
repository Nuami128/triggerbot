package com.example.triggerbot.module.impl;

import com.example.triggerbot.module.EmptyModule;
import net.minecraft.client.MinecraftClient;

public class AutoSprintModule extends EmptyModule {

    private int attackCooldown = 0;

    public AutoSprintModule() {
        super("AutoSprint");
    }

    @Override
    public String getName() { return "AutoSprint"; }

    @Override
    public void onEnable() {}

    @Override
    public void onDisable() {}

    public void onAttack() {
        attackCooldown = 3;
    }

    @Override
    public void onTick() {
        MinecraftClient mc = MinecraftClient.getInstance();

        if (mc.player == null) return;
        if (mc.currentScreen != null) return;
        if (mc.player.isDead()) return;
        if (mc.player.isUsingItem()) return;
        if (mc.player.isBlocking()) return;
        if (mc.player.isTouchingWater()) return;
        if (mc.player.getHungerManager().getFoodLevel() <= 6) return;
        if (!mc.options.forwardKey.isPressed()) return;
        if (mc.player.getAttackCooldownProgress(0f) < 1.0f) return;

        if (attackCooldown > 0) {
            attackCooldown--;
            return;
        }

        mc.player.setSprinting(true);
    }

    @Override
    public void onPostMovement() {}
}
