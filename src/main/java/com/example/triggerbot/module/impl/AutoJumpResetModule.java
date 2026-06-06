package com.example.triggerbot.module.impl;

import com.example.triggerbot.module.EmptyModule;
import net.minecraft.client.MinecraftClient;

public class AutoJumpResetModule extends EmptyModule {

    private boolean releaseJump = false;
    private int lastHurtTime = 0;

    public AutoJumpResetModule() {
        super("Auto Jump Reset");
    }

    @Override
    public void onEnable() {
        super.onEnable();
        releaseJump = false;
        lastHurtTime = 0;
    }

    @Override
    public void onDisable() {
        super.onDisable();
        releaseJump = false;
        lastHurtTime = 0;
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc != null && mc.options != null) mc.options.jumpKey.setPressed(false);
    }

    @Override public void onTick() {}
    @Override public void onClientTick() {}
    @Override public void onAttack() {}
    @Override public void onJumpReset() {}
    @Override public void onDamage() {}

    // onPostMovement fires at TAIL — after the full tick and sendMovementPackets.
    // hurtTime is updated by the server packet handler during the tick, so by
    // TAIL it has already been decremented. Reading it here guarantees we catch
    // the 10→9 transition reliably every time we take a hit.
    @Override
    public void onPostMovement() {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc == null || mc.player == null) return;
        if (!isEnabled()) return;

        if (releaseJump) {
            mc.options.jumpKey.setPressed(false);
            releaseJump = false;
        }

        int hurtTime = mc.player.hurtTime;

        System.out.println("hurtTime=" + hurtTime + " lastHurtTime=" + lastHurtTime);
        if (hurtTime == 9 && lastHurtTime == 10) {
            mc.options.jumpKey.setPressed(true);
            releaseJump = true;
        }

        lastHurtTime = hurtTime;
    }
}
