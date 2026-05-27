package com.example.triggerbot.module.impl;

import com.example.triggerbot.module.EmptyModule;
import net.minecraft.client.MinecraftClient;
import net.minecraft.text.Text;
import net.minecraft.util.math.Vec3d;

public class AutoJumpResetModule extends EmptyModule {

    private enum State {
        IDLE,
        DISRUPTED,
        READY,
        FIRED
    }

    private State state = State.IDLE;

    private Vec3d lastVelocity = Vec3d.ZERO;

    private int disruptionTicks = 0;
    private int readyTicks = 0;
    private int jumpHoldTicks = 0;

    // ✅ NEW: proper damage edge detection
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

        Vec3d vel = mc.player.getVelocity();

        double speed = vel.length();

        /*
         * =========================
         * DAMAGE EDGE DETECTION (FIXED)
         * =========================
         */

        int hurt = mc.player.hurtTime;

        if (hurt > 0 && lastHurtTime == 0) {
            state = State.DISRUPTED;
            disruptionTicks = 3; // shorter + faster reaction window
            debug(mc, "DISRUPTED (EDGE)");
        }

        /*
         * =========================
         * DISRUPTION TIMER
         * =========================
         */

        if (disruptionTicks > 0) {
            disruptionTicks--;
        }

        /*
         * =========================
         * READY STATE (faster)
         * =========================
         */

        if (state == State.DISRUPTED && disruptionTicks <= 0) {

            state = State.READY;

            readyTicks = 0; // removed artificial delay

            debug(mc, "READY");
        }

        /*
         * =========================
         * EXECUTION
         * =========================
         */

        if (state == State.READY) {

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

        /*
         * =========================
         * UPDATE STATE TRACKING
         * =========================
         */

        lastVelocity = vel;
        lastHurtTime = hurt;

        debugActionBar(mc,
                "STATE=" + state +
                        " | hurt=" + hurt +
                        " | speed=" + String.format("%.3f", speed)
        );
    }

    /*
     * =========================
     * EXECUTE JUMP RESET
     * =========================
     */

    private void triggerJump(MinecraftClient mc) {

        mc.options.jumpKey.setPressed(true);

        jumpHoldTicks = 2;

        debug(mc, "FIRED RESET");
    }

    /*
     * =========================
     * RESET STATE
     * =========================
     */

    private void reset() {

        state = State.IDLE;

        disruptionTicks = 0;
        readyTicks = 0;
        jumpHoldTicks = 0;

        lastVelocity = Vec3d.ZERO;
        lastHurtTime = 0;
    }

    /*
     * =========================
     * DEBUG CHAT
     * =========================
     */

    private void debug(MinecraftClient mc, String msg) {

        if (mc.player != null) {
            mc.player.sendMessage(Text.of("JR: " + msg), true);
        }

        System.out.println("[AutoJR] " + msg);
    }

    /*
     * =========================
     * DEBUG ACTION BAR
     * =========================
     */

    private void debugActionBar(MinecraftClient mc, String msg) {

        if (mc.player != null) {
            mc.player.sendMessage(Text.of(msg), true);
        }
    }
}
