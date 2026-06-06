package com.example.triggerbot.module.impl;

import com.example.triggerbot.TriggerBotMod;
import com.example.triggerbot.module.ClientModule;
import com.example.triggerbot.util.CombatUtil;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Hand;

public class TriggerBotModule implements ClientModule {

    private final AutoStunModule autoStun;
    private final AutoSprintModule autoSprint;

    private boolean enabled = false;
    private long lastProcessedTick = -1L;
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
    @Override public String getName() { return "TriggerBot"; }
    @Override public boolean isEnabled() { return enabled; }

    @Override
    public void onEnable() {
        enabled = true;
        wasAirborne = false;
        lastVelY = 0;
        itemReleaseCooldown = 0;
        lastProcessedTick = -1L;
        releaseDelay = 0;
    }

    @Override
    public void onDisable() {
        enabled = false;
        releaseDelay = 0;
        lastProcessedTick = -1L;
        wasAirborne = false;
        lastVelY = 0;
        itemReleaseCooldown = 0;
    }

    @Override
    public void onTick() {}

    @Override
    public void onPostMovement() {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (!enabled) return;
        if (mc.player == null || mc.world == null) return;
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
        boolean hasMovement = (velX * velX + velZ * velZ) > 0.001;
        boolean falling = (velY <= -0.1) || (wasAirborne && lastVelY <= -0.1);

        // Skip exact landing tick from a real fall to avoid Simulation flags
        if (wasAirborne && onGround && lastVelY <= -0.1) {
            wasAirborne = airborne;
            lastVelY = velY;
            return;
        }

        wasAirborne = airborne;
        lastVelY = velY;

        if (ascending) return;
        if (airborne && !falling) return;
        if (onGround && !hasMovement) return;

        long currentTick = mc.world.getTime();
        if (currentTick == lastProcessedTick) return;
        lastProcessedTick = currentTick;

        if (mc.player.getAttackCooldownProgress(1.0f) < 0.85f) return;

        Entity target = mc.targetedEntity;
        if (target == null) return;
        if (!(target instanceof LivingEntity)) return;
        if (!target.isAlive() || target.isRemoved()) return;
        if (!CombatUtil.isInReach(mc, target)) return;
        if (target instanceof PlayerEntity pe && pe.isBlocking()
                && CombatUtil.isFacingUs(mc, target) && !autoStun.isEnabled()) {
            autoStun.onEnable();
            return;
        }

        // Replace invokeDoAttack() with direct calls in the order Grim expects:
        // 1. attackEntity  → sends INTERACT_ENTITY packet
        // 2. swingHand     → sends ANIMATION packet
        // This matches vanilla packet order exactly and fixes Post/PacketOrderO flags.
        mc.interactionManager.attackEntity(mc.player, target);
        mc.player.swingHand(Hand.MAIN_HAND);

        autoSprint.onAttack();
        TriggerBotMod.getModuleManager().onAttackAll();
    }
}
