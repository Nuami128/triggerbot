package com.example.triggerbot.module.impl;

import com.example.triggerbot.module.EmptyModule;
import net.minecraft.client.MinecraftClient;

public class AutoSprintModule extends EmptyModule {

    private int attackBufferTicks = 0;

    public AutoSprintModule() {
        super("AutoSprint");
    }

    @Override
    public String getName() { return "AutoSprint"; }

    @Override
    public void onEnable() {
        attackBufferTicks = 0;
    }

    @Override
    public void onDisable() {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc != null && mc.player != null) {
            mc.player.setSprinting(false);
        }
    }

    // Triggered immediately by TriggerBot right during a hit
    public void onAttack() {
        attackBufferTicks = 2; // Pause automatic sprinting completely for 2 ticks to give Grim simulation room to settle
    }

    @Override
    public void onTick() {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc == null || mc.player == null) return;
        if (!isEnabled()) return;

        // Count down the attack recovery phase
        if (attackBufferTicks > 0) {
            attackBufferTicks--;
            if (mc.player.isSprinting()) {
                mc.player.setSprinting(false); // Cleanly drop sprint locally without sending malformed command loops
            }
            return;
        }

        // Vanilla validation requirements before forcing sprinting
        if (mc.options.forwardKey.isPressed() 
                && !mc.player.isSprinting() 
                && !mc.player.isUsingItem() 
                && !mc.player.isHorizontalCollision()
                && mc.player.getHungerManager().getFoodLevel() > 6) {
            
            // Set the vanilla client's intentional sprint key state change
            mc.player.setSprinting(true);
        }
    }

    @Override public void onPostMovement() {}
    @Override public void onJumpReset() {}
}

