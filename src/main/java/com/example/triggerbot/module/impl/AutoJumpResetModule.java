package com.example.triggerbot.module.impl;

import com.example.triggerbot.module.EmptyModule;
import net.minecraft.client.MinecraftClient;

public class AutoJumpResetModule extends EmptyModule {

    private int cooldown = 0;
    private int pendingJump = -1;
    private int lastHurtTime = 0;

    public AutoJumpResetModule() {
        super("Auto Jump Reset");
    }

    @Override
    public void onTick() {
        if (cooldown > 0) cooldown--;

        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc == null || mc.player == null) return;
        if (!isEnabled()) return;

        int hurtTime = mc.player.hurtTime;

        // FIX: Verify that YOUR player actually has an active attacker forcing your damage state
        boolean isBeingAttacked = mc.player.getAttacker() != null || mc.player.getLastAttacker() != null;

        if (hurtTime == 9 && lastHurtTime != 9 && cooldown == 0 && isBeingAttacked) {
            // Safe randomizer delay of 0 to 1 ticks
            pendingJump = (int)(Math.random() * 2); 
        }
        lastHurtTime = hurtTime;

        // Process the delayed jump
        if (pendingJump >= 0) {
            if (pendingJump == 0) {
                mc.player.input.jump();
                cooldown = 10; // 10 tick cooldown to prevent false multi-jump flags
                pendingJump = -1;
            } else {
                pendingJump--;
            }
        }
    }

    @Override public void onAttack() {}
    @Override public void onClientTick() {}
    @Override public void onJumpReset() {}
    @Override public void onPostMovement() {}
    @Override public void onDamage() {}
}

