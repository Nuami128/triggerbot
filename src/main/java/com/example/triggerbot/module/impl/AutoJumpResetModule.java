package com.example.triggerbot.module.impl;

import com.example.triggerbot.module.EmptyModule;
import net.minecraft.client.MinecraftClient;
import net.minecraft.util.PlayerInput;

public class AutoJumpResetModule extends EmptyModule {

    private int cooldown = 0;
    private boolean shouldReset = false;

    public AutoJumpResetModule() {
        super("Auto Jump Reset");
    }

    @Override 
    public void onDamage() {
        if (!isEnabled() || cooldown > 0) return;
        shouldReset = true; 
    }

    @Override
    public void onTick() {
        if (cooldown > 0) cooldown--;

        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc == null || mc.player == null) return;

        if (shouldReset && cooldown == 0) {
            PlayerInput current = mc.player.input.playerInput;
            
            mc.player.input.playerInput = new PlayerInput(
                current.forward(),
                current.backward(),
                current.left(),
                current.right(),
                true, // Force Jump Packet Input
                current.sneak(),
                current.sprint()
            );

            cooldown = 10;      
            shouldReset = false; 
        }
        
        if (cooldown > 0) {
            shouldReset = false;
        }
    }

    @Override public void onAttack() {}
    @Override public void onClientTick() {}
    @Override public void onJumpReset() {}
    @Override public void onPostMovement() {}
}
