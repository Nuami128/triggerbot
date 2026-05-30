package com.example.triggerbot.module.impl;

import com.example.triggerbot.module.EmptyModule;
import net.minecraft.client.MinecraftClient;

public class AutoJumpResetModule extends EmptyModule {

    private boolean hasQueuedRelease = false;

    public AutoJumpResetModule() {
        super("Auto Jump Reset");
    }

    // Called instantly the exact microsecond the hit packet is processed by the client
    @Override
    public void onDamage() {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc == null || mc.player == null) return;
        if (mc.currentScreen != null || mc.player.isUsingItem()) return;

        // If we are securely standing on a block when the damage strikes
        if (mc.player.isOnGround()) {
            // 1. VANILLA SIMULATION: Physically activate the game's actual macro binding key.
            // Because we toggle it instantly inside the network thread, the very next physics 
            // calculation block registers it as an intentional vanilla user keystroke.
            mc.options.jumpKey.setPressed(true);
            mc.player.setJumping(true);
            
            hasQueuedRelease = true;
            System.out.println("[AutoJR] Network Damage Intercepted! Keybind Pressed.");
        }
    }

    // Use your existing Pre-Movement Mixin hook to release the spacebar 
    // precisely 1 frame later so your character does not bounce repeatedly!
    @Override
    public void onJumpReset() {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc == null || mc.player == null) return;

        if (hasQueuedRelease && !mc.player.isOnGround()) {
            mc.options.jumpKey.setPressed(false);
            hasQueuedRelease = false;
            System.out.println("[AutoJR] Safe Vanilla Key Release Triggered.");
        }
    }

    @Override
    public void onTick() {}

    @Override
    public void onPostMovement() {}
}
