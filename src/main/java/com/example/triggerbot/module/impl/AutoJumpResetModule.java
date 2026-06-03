package com.example.triggerbot.module.impl;

import com.example.triggerbot.module.EmptyModule;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.input.PlayerInput; // Required for 1.21.11

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
        
        // FIX 1: STRICT VELOCITY CHECK
        // Only trigger if we have Vertical Velocity (Knockback lifts you up). 
        // We ignore X/Z so sprinting doesn't trigger it.
        boolean tookVerticalKnockback = mc.player.getVelocity().y > 0.0001;

        if (hurtTime == 9 && lastHurtTime != 9 && cooldown == 0 && tookVerticalKnockback) {
            // Random delay (0-1 ticks) to look human
            pendingJump = (int)(Math.random() * 2); 
        }
        lastHurtTime = hurtTime;

        if (pendingJump >= 0) {
            if (pendingJump == 0) {
                // FIX 2: PACKET COMPLIANT JUMP
                // We recreate the record so the server SEES the input packet.
                // This prevents Grim "Simulation" flags.
                PlayerInput current = mc.player.input.playerInput;
                mc.player.input.playerInput = new PlayerInput(
                    current.forward(),
                    current.backward(),
                    current.left(),
                    current.right(),
                    true, // JUMP = TRUE
                    current.sneak(),
                    current.sprint()
                );
                
                cooldown = 10; 
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
