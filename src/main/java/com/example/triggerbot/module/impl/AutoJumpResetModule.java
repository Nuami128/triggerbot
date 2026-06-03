package com.example.triggerbot.module.impl;

import com.example.triggerbot.module.EmptyModule;
import net.minecraft.client.MinecraftClient;
import net.minecraft.util.PlayerInput;

public class AutoJumpResetModule extends EmptyModule {

    private int cooldown = 0;
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

        // 1. CHOOSE LOCAL PLAYER STATE
        // This targets YOUR local character model flashing red, completely ignoring your crosshair targets.
        int localHurtTime = mc.player.hurtTime;

        // 2. DETECT LOCAL DAMAGE FRAME
        // Fires precisely when YOUR hurtTime drops to 9, indicating you took a hit 50ms ago.
        if (localHurtTime == 9 && lastHurtTime != 9 && cooldown == 0) {
            PlayerInput current = mc.player.input.playerInput;
            
            // Re-instantiate the packet record so GrimAC matches the simulation perfectly
            mc.player.input.playerInput = new PlayerInput(
                current.forward(),
                current.backward(),
                current.left(),
                current.right(),
                true, // Force Jump Packet Input
                current.sneak(),
                current.sprint()
            );

            cooldown = 10; // 500ms immunity window to prevent multi-hit kickback flags
        }
        
        lastHurtTime = localHurtTime;
    }

    // Clean up empty template layers to avoid event pipeline overlaps
    @Override public void onAttack() {}
    @Override public void onClientTick() {}
    @Override public void onJumpReset() {}
    @Override public void onPostMovement() {}
    @Override public void onDamage() {} 
}
