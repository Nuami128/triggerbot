package com.example.triggerbot.module.impl;

import com.example.triggerbot.module.EmptyModule;
import com.example.triggerbot.mixinterface.KeyBindingAccessor; // Ensure this matches your accessor's package
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;

public class AutoJumpResetModule extends EmptyModule {

    private int cooldown = 0;
    private int pendingJump = -1;

    public AutoJumpResetModule() {
        super("Auto Jump Reset");
    }

    @Override
    public void onTick() {
        if (cooldown > 0) cooldown--;

        if (pendingJump > 0) {
            pendingJump--;
            if (pendingJump == 0) {
                MinecraftClient mc = MinecraftClient.getInstance();
                if (mc != null && mc.player != null && mc.player.isOnGround()) {
                    
                    // --- SIMULATED HARDWARE JUMP CONCEPT ---
                    KeyBinding jumpKey = mc.options.jumpKey;

                    // 1. Force the engine to register the key as physically held
                    jumpKey.setPressed(true);

                    // 2. Safely grab the bound key mapping via your Mixin Accessor
                    InputUtil.Key boundKey = ((KeyBindingAccessor) jumpKey).getBoundKey();

                    // 3. Fire the game's actual internal keypress event handler
                    KeyBinding.onKeyPressed(boundKey);

                    // 4. Instantly release it so the player doesn't bounce continuously
                    jumpKey.setPressed(false);
                    // ---------------------------------------

                    cooldown = 10;
                }
                pendingJump = -1;
            }
        }
    }

    @Override
    public void onAttack() {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc == null || mc.player == null) return;
        if (!isEnabled()) return;
        if (!mc.player.isOnGround()) return;
        if (mc.targetedEntity == null) return;
        if (cooldown > 0) return;

        // Random 1-3 tick delay before jumping
        pendingJump = 1 + (int)(Math.random() * 3);
    }

    @Override public void onClientTick() {}
    @Override public void onJumpReset() {}
    @Override public void onPostMovement() {}
    @Override public void onDamage() {}
}
