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
    private int jumpHoldTicks = 0;

    public AutoJumpResetModule() {
        super("Smart Jump Reset");
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

        // detect movement disruption
        if (delta > 0.15) {
            state = State.DISRUPTED;
            disruptionTicks = 6;
        }

        if (disruptionTicks > 0) {
            disruptionTicks--;
        }

        if (state == State.DISRUPTED && disruptionTicks <= 0) {
            state = State.READY;
        }

        // execute jump reset
        if (state == State.READY) {

            if (mc.player.isOnGround() && speed > 0.05) {
                triggerJump(mc);
                state = State.FIRED;
            }
        }

        // jump hold simulation
        if (jumpHoldTicks > 0) {
            mc.options.jumpKey.setPressed(true);
            jumpHoldTicks--;

            if (jumpHoldTicks == 0) {
                mc.options.jumpKey.setPressed(false);
            }
        }

        lastVelocity = vel;

        debug(mc, "STATE: " + state + " | speed=" + speed);
    }

    private void triggerJump(MinecraftClient mc) {
        mc.options.jumpKey.setPressed(true);
        jumpHoldTicks = 2;

        debug(mc, "FIRED RESET");
    }

    private void reset() {
        state = State.IDLE;
        disruptionTicks = 0;
        jumpHoldTicks = 0;
        lastVelocity = Vec3d.ZERO;
    }

    private void debug(MinecraftClient mc, String msg) {
        if (mc.player != null) {
            mc.player.sendMessage(Text.of(msg), true);
        }
        System.out.println("[SmartJR] " + msg);
    }
}
