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
        double lastSpeed = lastVelocity.length();

        double delta = Math.abs(speed - lastSpeed);

        /*
         * =========================
         * MOVEMENT DISRUPTION DETECTION
         * =========================
         */

        // only react if movement changed AND we are hurt
        if (delta > 0.15 && mc.player.hurtTime > 0) {

            if (state == State.IDLE || state == State.FIRED) {
                state = State.DISRUPTED;
                disruptionTicks = 6;

                debug(mc, "DISRUPTED");
            }
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
         * READY STATE
         * =========================
         */

        if (state == State.DISRUPTED && disruptionTicks <= 0) {

            state = State.READY;

            // tiny stabilization delay
            readyTicks = 2;

            debug(mc, "READY");
        }

        /*
         * =========================
         * READY TIMER
         * =========================
         */

        if (state == State.READY) {

            if (readyTicks > 0) {
                readyTicks--;
            } else {

                // only jump if grounded and moving
                if (mc.player.isOnGround() && speed > 0.05) {

                    triggerJump(mc);

                    state = State.FIRED;
                }
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

                // return to idle after release
                state = State.IDLE;
            }
        }

        lastVelocity = vel;

        debugActionBar(mc,
                "STATE=" + state +
                " | delta=" + String.format("%.3f", delta) +
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

        // simulate short human tap
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
