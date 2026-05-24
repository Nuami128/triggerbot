package com.example.triggerbot.module.impl;

import com.example.triggerbot.module.ClientModule;
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

public class TriggerBotModule implements ClientModule {

    private final AutoStunModule autoStun;

    private boolean enabled = false;
    private long lastProcessedTick = -1L;
    private int cooldownTicks = 0;
    private int releaseDelay = 0;

    // Damage tracking for punish crits
    private float lastHealth = -1f;
    private boolean recentlyHit = false;
    private int hitCooldown = 0;

    // Sprint reset state
    private boolean sprintResetPending = false;

    public TriggerBotModule(AutoStunModule autoStun) {
        this.autoStun = autoStun;
    }

    @Override
    public String getName() { return "TriggerBot"; }

    public boolean isEnabled() { return enabled; }

    @Override
    public void onEnable() {
        enabled = true;
        lastHealth = -1f;
        recentlyHit = false;
        hitCooldown = 0;
        sprintResetPending = false;
    }

    @Override
    public void onDisable() {
        enabled = false;
        cooldownTicks = 0;
        releaseDelay = 0;
        lastProcessedTick = -1L;
        recentlyHit = false;
        hitCooldown = 0;
        sprintResetPending = false;
    }

    @Override
    public void onTick() {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (!enabled || mc.player == null) return;

        // Track health to detect when we take damage
        float currentHealth = mc.player.getHealth();
        if (lastHealth > 0 && currentHealth < lastHealth) {
            recentlyHit = true;
            hitCooldown = 12;
        }
        lastHealth = currentHealth;

        if (hitCooldown > 0) hitCooldown--;
        if (hitCooldown == 0) recentlyHit = false;
    }

    @Override
    public void onPostMovement() {
        MinecraftClient mc = MinecraftClient.getInstance();

        if (!enabled) return;
        if (mc.player == null || mc.world == null) return;
        if (mc.currentScreen != null) return;
        if (mc.player.isDead()) return;
        if (mc.interactionManager == null) return;
        if (mc.getNetworkHandler() == null) return;

        if (CombatUtil.isPlayerBusy(mc)) {
            releaseDelay = 2;
            return;
        }

        if (releaseDelay > 0) {
            releaseDelay--;
            return;
        }

        // Only fire with sword or axe
        ItemStack held = mc.player.getMainHandStack();
        if (!CombatUtil.isSword(held) && !CombatUtil.isAxe(held)) return;

        double velY = mc.player.getVelocity().y;
        double velX = mc.player.getVelocity().x;
        double velZ = mc.player.getVelocity().z;
        boolean onGround = mc.player.isOnGround();
        boolean ascending = velY > 0;
        boolean falling = velY < 0;
        boolean airborne = !onGround;
        boolean sprinting = mc.player.isSprinting();

        // Horizontal movement — low threshold to block stationary sweep only
        boolean hasMovement = (velX * velX + velZ * velZ) > 0.001;

        // Never attack while ascending
        if (ascending) return;

        // On ground: must be sprinting AND moving
        if (onGround && !sprinting) return;
        if (onGround && !hasMovement) return;

        // Airborne: must be falling for crits
        if (airborne && !falling) return;

        long currentTick = mc.world.getTime();
        if (currentTick == lastProcessedTick) return;
        lastProcessedTick = currentTick;

        if (cooldownTicks > 0) {
            cooldownTicks--;
            return;
        }

        // Sprint reset for crits — stop sprint 1 tick before attacking when airborne
        if (airborne && falling && sprinting) {
            if (!sprintResetPending) {
                mc.player.setSprinting(false);
                sprintResetPending = true;
                return;
            }
        }
        sprintResetPending = false;

        // Cooldown threshold
        float cooldownThreshold;
        if (airborne && falling) {
            cooldownThreshold = 1.0f;
        } else if (recentlyHit) {
            cooldownThreshold = 0.60f;
        } else {
            cooldownThreshold = 0.85f;
        }

        if (mc.player.getAttackCooldownProgress(1.0f) < cooldownThreshold) return;

        Entity target = findTarget(mc);
        if (target == null) return;

        // Trigger AutoStun if target is shielding and facing us
        if (target instanceof PlayerEntity pe
                && pe.isBlocking()
                && CombatUtil.isFacingUs(mc, target)
                && !autoStun.isEnabled()) {
            autoStun.onEnable();
            cooldownTicks = 1;
            return;
        }

        mc.interactionManager.attackEntity(mc.player, target);
        mc.player.swingHand(Hand.MAIN_HAND);

        // Re-enable sprint after attack for knockback
        mc.player.setSprinting(true);

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
            if (hit.isPresent()) return e;
        }
        return null;
    }
}
