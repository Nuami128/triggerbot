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
    public void onTick() {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc == null || mc.player == null) return;
        if (!isEnabled()) return;

        // Release jump key the tick after we pressed it (tap behaviour)
        if (releaseJump) {
            mc.options.jumpKey.setPressed(false);
            releaseJump = false;
        }

        boolean onGround = mc.player.isOnGround();

        // Jump the moment we touch the ground (bunnyhop)
        // wasOnGround prevents re-triggering while already standing still
        if (onGround && !wasOnGround) {
            mc.options.jumpKey.setPressed(true);
            releaseJump = true;
        }

        wasOnGround = onGround;
    }

    @Override public void onAttack() {}
    @Override public void onClientTick() {}
    @Override public void onJumpReset() {}
    @Override public void onPostMovement() {}
    @Override public void onDamage() {}
}
