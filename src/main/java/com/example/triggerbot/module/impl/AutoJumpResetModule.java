package com.example.triggerbot.module.impl;

import com.example.triggerbot.module.EmptyModule;
import net.minecraft.client.MinecraftClient;
import net.minecraft.text.Text;

public class AutoJumpResetModule extends EmptyModule {

    private int prevHurtTime = 0;

    private int pendingJumpTicks = 0;
    private int jumpHeldTicks = 0;

    public AutoJumpResetModule() {
        super("Auto Jump Reset");
    }

    @Override
    public void onTick() {

        MinecraftClient mc = MinecraftClient.getInstance();

        if (mc == null || mc.player == null || mc.world == null) {
            prevHurtTime = 0;
            return;
        }

        /*
         * =========================
         * 1. HIT DETECTION
         * =========================
         * hurtTime == 10 means "just got hit"
         */

        int hurtTime = mc.player.hurtTime;

        if (hurtTime == 10 && prevHurtTime != 10) {
            onHit(mc);
        }

        prevHurtTime = hurtTime;

        /*
         * =========================
         * 2. JUMP HOLD LOGIC
         * =========================
         * Holds jump for a short burst
         */

        if (jumpHeldTicks > 0) {
            mc.options.jumpKey.setPressed(true);
            jumpHeldTicks--;

            if (jumpHeldTicks == 0) {
                mc.options.jumpKey.setPressed(false);
            }
        }

        /*
         * =========================
         * 3. EXECUTE JUMP RESET
         * =========================
         */

        if (pendingJumpTicks > 0) {

            if (mc.player.isOnGround()) {

                debug(mc, "JUMP RESET FIRED");

                jumpHeldTicks = 2;      // simulate real press
                pendingJumpTicks = 0;   // stop waiting

            } else {
                pendingJumpTicks--;
            }
        }
    }

    /*
     * =========================
     * HIT EVENT LOGIC
     * =========================
     */

    private void onHit(MinecraftClient mc) {

        if (mc.player.isUsingItem()) {
            debug(mc, "SKIP: using item");
            return;
        }

        if (mc.player.getVelocity().horizontalLengthSquared() < 0.0005) {
            debug(mc, "SKIP: not moving");
            return;
        }

        pendingJumpTicks = 5;
        debug(mc, "HIT DETECTED → jump armed");
    }

    /*
     * =========================
     * DEBUG MESSAGE
     * =========================
     */

    private void debug(MinecraftClient mc, String msg) {
        if (mc.player != null) {
            mc.player.sendMessage(Text.literal("JR: " + msg), true);
        }
        System.out.println("[AutoJumpReset] " + msg);
    }
}
