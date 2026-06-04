package com.example.triggerbot.module.impl;

import com.example.triggerbot.module.EmptyModule;
import net.minecraft.client.MinecraftClient;

public class AutoSprintModule extends EmptyModule {

    public AutoSprintModule() {
        super("AutoSprint");
    }

    @Override
    public String getName() { return "AutoSprint"; }

    @Override
    public void onEnable() {}

    @Override
    public void onDisable() {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player != null) mc.player.setSprinting(false);
    }

    public void onAttack() {}

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
        return true;
    }

    @Override
    public void onTick() {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player == null) return;

        boolean should = shouldSprint(mc);

        if (should && !mc.player.isSprinting()) {
            mc.player.setSprinting(true);
        } else if (!should && mc.player.isSprinting()) {
            mc.player.setSprinting(false);
        }
    }

    @Override public void onPostMovement() {}
    @Override public void onJumpReset() {}
}
