package com.example.triggerbot.module.impl;

import com.example.triggerbot.module.EmptyModule;
import net.minecraft.client.MinecraftClient;
import net.minecraft.text.Text;
import net.minecraft.util.math.Vec3d;

public class AutoJumpResetModule extends EmptyModule {

    private enum State {
        IDLE,
        READY,
        FIRED
    }

    private State state = State.IDLE;

    private int jumpHoldTicks = 0;

    // damage edge tracking
    private int lastHurtTime = 0;

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

        /*
         * =========================
         * INSTANT DAMAGE EDGE
         * =========================
         */

        if (hurt > 0 && lastHurtTime == 0) {

            state = State.READY;

            debug(mc, "READY (FAST)");

            // optional: immediate execution if grounded
            if (mc.player.isOnGround()) {
                triggerJump(mc);
                state = State.FIRED;
            }
        }

        /*
         * =========================
         * JUMP HOLD
         * =========================
         */

        if (jumpHoldTicks > 0) {

            mc.options.jumpKey.setPressed(true);

            jumpHoldTicks--;

            if (jumpHoldTicks == 0) {
                mc.options.jumpKey.setPressed(false);
                state = State.IDLE;
            }
        }

        lastHurtTime = hurt;

        debugActionBar(mc, "STATE=" + state + " | hurt=" + hurt);
    }

    /*
     * =========================
     * EXEC JUMP
     * =========================
     */

    private void triggerJump(MinecraftClient mc) {

        mc.options.jumpKey.setPressed(true);

        jumpHoldTicks = 1; // minimal input simulation

        debug(mc, "FIRED");
    }

    private void reset() {

        state = State.IDLE;
        jumpHoldTicks = 0;
        lastHurtTime = 0;
    }

    private void debug(MinecraftClient mc, String msg) {

        if (mc.player != null) {
            mc.player.sendMessage(Text.of("JR: " + msg), true);
        }

        System.out.println("[AutoJR] " + msg);
    }

    private void debugActionBar(MinecraftClient mc, String msg) {

        if (mc.player != null) {
            mc.player.sendMessage(Text.of(msg), true);
        }
    }
}
