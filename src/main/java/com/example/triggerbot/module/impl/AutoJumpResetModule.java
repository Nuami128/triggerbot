package com.example.triggerbot.module.impl;

import com.example.triggerbot.module.EmptyModule;
import net.minecraft.client.MinecraftClient;

public class AutoJumpResetModule extends EmptyModule {

    private boolean releaseJump = false;
    private boolean wasOnGround = false;

    public AutoJumpResetModule() {
        super("Auto Jump Reset");
    }

    @Override
    public void onEnable() {
        super.onEnable();
        releaseJump = false;
        wasOnGround = false;
    }

    @Override
    public void onDisable() {
        super.onDisable();
        releaseJump = false;
        wasOnGround = false;
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc != null) mc.options.jumpKey.setPressed(false);
    }

    @Override
    public void onTick() {}

    @Override
    public void onClientTick() {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc == null || mc.player == null) return;
        if (!isEnabled()) return;

        // Release the jump key the tick after we pressed it
        if (releaseJump) {
            mc.options.jumpKey.setPressed(false);
            releaseJump = false;
        }

        wasOnGround = mc.player.isOnGround();
    }

    // Fires via PlayerDamageMixin → ModuleManager.onDamageAll()
    // when the local player takes a hit. Jump resets unconditionally —
    // no target or ground state check needed since taking damage means
    // combat is already happening.
    @Override
    public void onDamage() {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc == null || mc.player == null) return;
        if (!isEnabled()) return;

        mc.options.jumpKey.setPressed(true);
        releaseJump = true;
    }

    @Override public void onAttack() {}
    @Override public void onJumpReset() {}
    @Override public void onPostMovement() {}
}
