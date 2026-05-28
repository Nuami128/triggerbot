package com.example.triggerbot.module.impl;

import com.example.triggerbot.module.EmptyModule;
import com.example.triggerbot.util.CombatUtil;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Hand;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;

import java.util.Optional;

public class TriggerBotModule extends EmptyModule {

    private final AutoStunModule autoStun;

    private long lastProcessedTick = -1L;
    private int cooldownTicks = 0;
    private int releaseDelay = 0;

    // Damage tracking
    private float lastHealth = -1f;
    private boolean recentlyHit = false;
    private int hitCooldown = 0;

    // Crit tracking
    private boolean wasAirborne = false;
    private double lastVelY = 0;

    public TriggerBotModule(AutoStunModule autoStun) {
        super("TriggerBot");
        this.autoStun = autoStun;
    }

    @Override
    public void onEnable() {
        super.onEnable();

        lastHealth = -1f;
        recentlyHit = false;
        hitCooldown = 0;
        wasAirborne = false;
        lastVelY = 0;
    }

    @Override
    public void onDisable() {
        super.onDisable();

        cooldownTicks = 0;
        releaseDelay = 0;
        lastProcessedTick = -1L;
        recentlyHit = false;
        hitCooldown = 0;
        wasAirborne = false;
        lastVelY = 0;
    }

    @Override
    public void onTick() {

        MinecraftClient mc = MinecraftClient.getInstance();

        if (!isEnabled()) return;
        if (mc.player == null || mc.world == null) return;
        if (mc.currentScreen != null) return;
        if (mc.player.isDead()) return;
        if (mc.interactionManager == null) return;
        if (mc.getNetworkHandler() == null) return;

        // DAMAGE TRACKING

        float currentHealth = mc.player.getHealth();

        if (lastHealth > 0 && currentHealth < lastHealth) {
            recentlyHit = true;
            hitCooldown = 12;
        }

        lastHealth = currentHealth;

        if (hitCooldown > 0) hitCooldown--;
        if (hitCooldown == 0) recentlyHit = false;

        // BUSY CHECK

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

        // WEAPON CHECK

        ItemStack held = mc.player.getMainHandStack();

        if (!CombatUtil.isSword(held) && !CombatUtil.isAxe(held)) {
            return;
        }

        // MOVEMENT / CRIT CHECKS

        double velY = mc.player.getVelocity().y;
        double velX = mc.player.getVelocity().x;
        double velZ = mc.player.getVelocity().z;

        boolean onGround = mc.player.isOnGround();
        boolean ascending = velY > 0;
        boolean airborne = !onGround;
        boolean sprinting = mc.player.isSprinting();

        boolean hasMovement = (velX * velX + velZ * velZ) > 0.001;

        boolean falling = (velY <= -0.1)
                || (wasAirborne && lastVelY <= -0.1);

        wasAirborne = airborne;
        lastVelY = velY;

        if (ascending) return;
        if (onGround && !sprinting) return;
        if (onGround && !hasMovement) return;
        if (airborne && !falling) return;

        if (recentlyHit && airborne && velY > -0.1) {
            return;
        }

        // TICK LOCK

        long currentTick = mc.world.getTime();

        if (currentTick == lastProcessedTick) {
            return;
        }

        lastProcessedTick = currentTick;

        // COOLDOWN

        if (cooldownTicks > 0) {
            cooldownTicks--;
            return;
        }

        if (mc.player.getAttackCooldownProgress(1.0f) < 0.85f) {
            return;
        }

        // TARGET

        Entity target = findTarget(mc);

        if (target == null) {
            return;
        }

        // AUTO STUN

        if (target instanceof PlayerEntity pe
                && pe.isBlocking()
                && CombatUtil.isFacingUs(mc, target)
                && !autoStun.isEnabled()) {

            autoStun.onEnable();
            cooldownTicks = 1;
            return;
        }

        // ATTACK

        mc.interactionManager.attackEntity(mc.player, target);
        mc.player.swingHand(Hand.MAIN_HAND);

        // Sprint reset
        mc.player.setSprinting(false);

        cooldownTicks = 1;
    }

    private Entity findTarget(MinecraftClient mc) {

        Vec3d eyePos = mc.player.getEyePos();
        Vec3d look = mc.player.getRotationVec(1.0f);
        Vec3d reachVec = eyePos.add(look.multiply(3.0));

        for (Entity e : mc.world.getEntities()) {

            if (!(e instanceof LivingEntity)) continue;
            if (e == mc.player) continue;
            if (!e.isAlive()) continue;
            if (e.isRemoved()) continue;
            if (e.isSpectator()) continue;

            if (!CombatUtil.isInReach(mc, e)) continue;

            Box box = e.getBoundingBox();
            Optional<Vec3d> hit = box.raycast(eyePos, reachVec);

            if (hit.isPresent()) {
                return e;
            }
        }

        return null;
    }
}
