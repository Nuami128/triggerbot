package com.example.triggerbot.module.impl;

import com.example.triggerbot.module.EmptyModule;
import net.minecraft.client.MinecraftClient;

public class AutoSprintModule extends EmptyModule {

    private int attackCooldown = 0;
    private boolean serverSprintState = false;

    public AutoSprintModule() {
        super("AutoSprint");
    }

    @Override
    public String getName() { return "AutoSprint"; }

    @Override
    public void onEnable() {
        attackCooldown = 0;
        serverSprintState = false;
    }

    @Override
    public void onDisable() {
        attackCooldown = 0;
        serverSprintState = false;
    }

    public void onAttack() {
        attackCooldown = 3;
    }

    private boolean shouldSprint(MinecraftClient mc) {
        if (mc.player == null) return false;
        if (mc.currentScreen != null) return false;
        if (mc.player.isDead()) return false;
        if (mc.player.isUsingItem()) return false;
        if (mc.player.isBlocking()) return false;
        if (mc.player.isTouchingWater()) return false;
        if (mc.player.getHungerManager().getFoodLevel() <= 6) return false;
        if (!mc.options.forwardKey.isPressed()) return false;
        if (mc.player.getAttackCooldownProgress(0f) < 1.0f) return false;

        double velX = mc.player.getVelocity().x;
        double velZ = mc.player.getVelocity().z;
        if ((velX * velX + velZ * velZ) < 0.001) return false;

        return true;
    }

    @Override
    public void onTick() {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player == null) return;

        if (attackCooldown > 0) {
            attackCooldown--;
            if (serverSprintState) {
                mc.player.setSprinting(false);
                serverSprintState = false;
            }
            return;
        }

        boolean should = shouldSprint(mc);

        if (should && !serverSprintState) {
            mc.player.setSprinting(true);
            serverSprintState = true;
        } else if (!should && serverSprintState) {
            mc.player.setSprinting(false);
            serverSprintState = false;
        }
    }

    @Override
    public void onPostMovement() {}
}
