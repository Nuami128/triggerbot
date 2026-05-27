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

    private int jumpHoldTicks = 0;

    // edge detection
    private int lastHurtTime = 0;

    // prevents READY spam every tick while hurtTime stays > 0
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

        /*
         * =========================
         * LOCK TIMER (prevents spam)
         * =========================
         */
        if (hurtLockTicks > 0) {
            hurtLockTicks--;
        }

        /*
         * =========================
         * TRUE DAMAGE EDGE DETECTION
         * =========================
         */
        if (hurt > 0 && lastHurtTime == 0 && hurtLockTicks == 0) {

            state = State.READY;

            hurtLockTicks = 10; // block re-trigger for a short window

            debug(mc, "READY (EDGE)");

            // optional immediate execution if grounded
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
         * UPDATE TRACKING
         * =========================
         */
        lastHurtTime = hurt;

        debugActionBar(mc, "STATE=" + state + " | hurt=" + hurt);
    }

    /*
     * =========================
     * EXECUTE JUMP RESET
     * =========================
     */
    private void triggerJump(MinecraftClient mc) {

        mc.options.jumpKey.setPressed(true);

        jumpHoldTicks = 1;

        debug(mc, "FIRED");
    }

    /*
     * =========================
     * RESET STATE
     * =========================
     */
    private void reset() {

        state = State.IDLE;

        jumpHoldTicks = 0;
        lastHurtTime = 0;
        hurtLockTicks = 0;
    }

    /*
     * =========================
     * DEBUG
     * =========================
     */
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
