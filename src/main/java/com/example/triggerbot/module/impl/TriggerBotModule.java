package com.example.triggerbot.module.impl;

import com.example.triggerbot.TriggerBotMod;
import com.example.triggerbot.module.ClientModule;
import com.example.triggerbot.util.CombatUtil;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;

import java.util.Optional;

public class TriggerBotModule implements ClientModule {

    private final AutoStunModule autoStun;
    private final AutoSprintModule autoSprint;

    private boolean enabled = false;
    private long lastProcessedTick = -1L;
    private int cooldownTicks = 0;
    private int releaseDelay = 0;
    private int itemReleaseCooldown = 0;

    private boolean wasAirborne = false;
    private double lastVelY = 0;

    public TriggerBotModule(AutoStunModule autoStun, AutoSprintModule autoSprint) {
        this.autoStun = autoStun;
        this.autoSprint = autoSprint;
    }

    @Override public void onJumpReset() {}
    @Override public void onClientTick() {}
    @Override public void onAttack() {}
    @Override public void onDamage() {}

    @Override
    public String getName() { return "TriggerBot"; }

    @Override
    public boolean isEnabled() { return enabled; }

    @Override
    public void onEnable() {
        enabled = true;
        wasAirborne = false;
        lastVelY = 0;
        itemReleaseCooldown = 0;
        lastProcessedTick = -1L;
        cooldownTicks = 0;
        releaseDelay = 0;
    }

    @Override
    public void onDisable() {
        enabled = false;
        cooldownTicks = 0;
        releaseDelay = 0;
        lastProcessedTick = -1L;
        wasAirborne = false;
        lastVelY = 0;
        itemReleaseCooldown = 0;

        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc != null && mc.options != null && mc.options.attackKey.isPressed()) {
            mc.options.attackKey.setPressed(false);
        }
    }

    @Override
    public void onTick() {}

    @Override
    public void onPostMovement() {
        MinecraftClient mc = MinecraftClient.getInstance();

        if (mc.player == null || mc.world == null) return;

        // Clean up emulated key states safely
        if (mc.options.attackKey.isPressed() && enabled) {
            mc.options.attackKey.setPressed(false);
        }

        if (!enabled) return;
        if (mc.currentScreen != null) return;
        if (mc.player.isDead()) return;
        if (mc.interactionManager == null) return;
        if (mc.getNetworkHandler() == null) return;

        if (mc.player.isUsingItem()) {
            itemReleaseCooldown = 3;
        }
        if (itemReleaseCooldown > 0) {
            itemReleaseCooldown--;
            return;
        }

        if (CombatUtil.isPlayerBusy(mc)) {
            releaseDelay = 2;
            wasAirborne = false;
            lastVelY = 0;
            return;
        }

        if (releaseDelay > 0) {
            releaseDelay--;
            return;
        }

        ItemStack held = mc.player.getMainHandStack();
        if (!CombatUtil.isSword(held) && !CombatUtil.isAxe(held)) return;

        double velY = mc.player.getVelocity().y;
        double velX = mc.player.getVelocity().x;
        double velZ = mc.player.getVelocity().z;

        boolean onGround = mc.player.isOnGround();
        boolean ascending = velY > 0;
        boolean airborne = !onGround;
        boolean sprinting = mc.player.isSprinting();
        boolean hasMovement = (velX * velX + velZ * velZ) > 0.001;
        boolean falling = (velY <= -0.1) || (wasAirborne && lastVelY <= -0.1);

        wasAirborne = airborne;
        lastVelY = velY;

        if (ascending) return;
        if (onGround && !sprinting) return;
        if (onGround && !hasMovement) return;
        if (airborne && !falling) return;

        long currentTick = mc.world.getTime();
        if (currentTick == lastProcessedTick) return;
        lastProcessedTick = currentTick;

        if (cooldownTicks > 0) {
            cooldownTicks--;
            return;
        }

        if (mc.player.getAttackCooldownProgress(1.0f)  hit = box.raycast(eyePos, reachVec);
            if (hit.isPresent()) return e;
        }

        return null;
    }
}

