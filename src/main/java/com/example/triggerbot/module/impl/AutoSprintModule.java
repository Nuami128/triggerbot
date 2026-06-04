package com.example.triggerbot.module.impl;

import com.example.triggerbot.module.EmptyModule;
import net.minecraft.client.MinecraftClient;

public class AutoSprintModule extends EmptyModule {

    private int sprintResetDelay = 0;

    public AutoSprintModule() {
        super("AutoSprint");
    }

    @Override
    public String getName() { return "AutoSprint"; }

    @Override
    public void onEnable() {
        sprintResetDelay = 0;
    }

    @Override
    public void onDisable() {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc != null && mc.player != null) {
            mc.player.setSprinting(false);
        }
    }

    // Call this from TriggerBot to pause sprinting adjustments for 1 tick after hitting
    public void onAttack() {
        sprintResetDelay = 1;
    }

    @Override
    public void onTick() {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc == null || mc.player == null) return;
        if (!isEnabled()) return;

        if (sprintResetDelay > 0) {
            sprintResetDelay--;
            return;
        }

        // Vanilla checks: Only allow sprint changes if moving forward and not blocked by hunger/effects
        if (mc.options.forwardKey.isPressed() 
                && !mc.player.isSprinting() 
                && !mc.player.isUsingItem() 
                && mc.player.getHungerManager().getFoodLevel() > 6) {
            mc.player.setSprinting(true);
        }
    }

    @Override public void onPostMovement() {}
    @Override public void onJumpReset() {}
}
