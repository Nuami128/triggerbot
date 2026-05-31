package com.example.triggerbot.module.impl;

import com.example.triggerbot.module.EmptyModule;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.player.PlayerEntity;

public class AutoJumpResetModule extends EmptyModule {

    private int combatTimer = 0;
    private static final int COMBAT_TIMEOUT = 40; // 2 seconds after last damage event

    public AutoJumpResetModule() {
        super("Auto Jump Reset");
    }

    @Override
    public void onTick() {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc == null || mc.player == null) return;

        if (combatTimer > 0) combatTimer--;

        PlayerEntity player = mc.player;

        // Only jump reset if recently in combat and on ground and moving
        if (combatTimer > 0 && player.isOnGround() && isMoving(player)) {
            mc.execute(player::jump);
        }
    }

    @Override
    public void onDamage() {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc == null || mc.player == null) return;

        // Reset combat timer every time damage occurs
        combatTimer = COMBAT_TIMEOUT;

        mc.execute(() -> {
            if (mc.player == null) return;
            if (mc.player.isOnGround()) {
                mc.player.jump();
            }
        });
    }

    private boolean isMoving(PlayerEntity player) {
        return player.getVelocity().horizontalLength() > 0.003;
    }

    @Override
    public void onJumpReset() {}

    @Override
    public void onPostMovement() {}
}
