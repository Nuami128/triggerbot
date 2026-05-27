package com.example.triggerbot.module.impl;

import com.example.triggerbot.module.EmptyModule;
import net.minecraft.client.MinecraftClient;
import net.minecraft.text.Text;

public class AutoJumpResetModule extends EmptyModule {

    private enum State {
        IDLE,
        READY,
        FIRED
    }

    private State state = State.IDLE;

    private int lastHurtTime = 0;
    private int hurtLockTicks = 0;

    public AutoJumpResetModule() {
        super("Auto Jump Reset");
    }

    @Override
    public void onTick() {

        MinecraftClient mc = MinecraftClient.getInstance();

        if (mc == null || mc.player == null || mc.world == null) {
            reset();
            return;
        }

        int hurt = mc.player.hurtTime;

        // cooldown to prevent spam triggers
        if (hurtLockTicks > 0) {
            hurtLockTicks--;
        }

        // DAMAGE EDGE DETECT
        if (hurt > 0 && lastHurtTime == 0 && hurtLockTicks == 0) {

            state = State.READY;
            hurtLockTicks = 10;

            debug(mc, "READY (DAMAGE DETECTED)");

            triggerJump(mc);
            state = State.FIRED;
        }

        lastHurtTime = hurt;

        debugActionBar(mc, "STATE=" + state + " | hurt=" + hurt);
    }

    /**
     * Executes jump reset
     */
    private void triggerJump(MinecraftClient mc) {

        if (mc.player == null) return;

        System.out.println("[AutoJR] TRIGGER FIRED");

        mc.player.jump(); // ✅ correct 1.21+ method

        debug(mc, "FIRED");
    }

    /**
     * Reset internal state
     */
    private void reset() {
        state = State.IDLE;
        lastHurtTime = 0;
        hurtLockTicks = 0;
    }

    /**
     * Chat debug (action bar style)
     */
    private void debug(MinecraftClient mc, String msg) {
        if (mc.player != null) {
            mc.player.sendMessage(Text.of("JR: " + msg), true);
        }
        System.out.println("[AutoJR] " + msg);
    }

    /**
     * Action bar spam (state display)
     */
    private void debugActionBar(MinecraftClient mc, String msg) {
        if (mc.player != null) {
            mc.player.sendMessage(Text.of(msg), true);
        }
    }
}
