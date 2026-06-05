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

    // onTick fires BEFORE sendMovementPackets — too early for a jump.
    // Grim sees the jump velocity before the movement packet, flags Simulation.
    @Override
    public void onTick() {}

    // onClientTick fires from ClientTickEvents.END_CLIENT_TICK — AFTER the
    // full game tick and movement packets are done. Grim's next prediction
    // will correctly account for the jump. This is why it must be here.
    @Override
    public void onClientTick() {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc == null || mc.player == null) return;
        if (!isEnabled()) return;

        // Release jump key the tick after pressing (one-tick tap)
        if (releaseJump) {
            mc.options.jumpKey.setPressed(false);
            releaseJump = false;
        }

        boolean onGround = mc.player.isOnGround();

        // Bunnyhop: fire the moment we land (airborne -> grounded transition)
        if (onGround && !wasOnGround) {
            mc.options.jumpKey.setPressed(true);
            releaseJump = true;
        }

        wasOnGround = onGround;
    }

    @Override public void onAttack() {}
    @Override public void onJumpReset() {}
    @Override public void onPostMovement() {}
    @Override public void onDamage() {}
}
