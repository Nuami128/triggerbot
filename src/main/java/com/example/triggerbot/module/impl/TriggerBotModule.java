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
    private int sprintTicks = 0;

    // 1-tick ground attack delay
    private boolean pendingGroundAttack = false;
    private Entity queuedGroundTarget = null;

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
        cooldownTicks = 0;
        releaseDelay = 0;
        sprintTicks = 0;
        pendingGroundAttack = false;
        queuedGroundTarget = null;
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
        sprintTicks = 0;
        pendingGroundAttack = false;
        queuedGroundTarget = null;
    }

    @Override
    public void onTick() {
        // No logic here — everything is driven by onPostMovement
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

        // Fire any pending ground attack from last tick first
        if (pendingGroundAttack && queuedGroundTarget != null) {
            Entity t = queuedGroundTarget;
            pendingGroundAttack = false;
            queuedGroundTarget = null;
            if (t.isAlive() && !t.isRemoved() && CombatUtil.isInReach(mc, t)) {
                mc.interactionManager.attackEntity(mc.player, t);
                mc.player.swingHand(Hand.MAIN_HAND);
                cooldownTicks = 1;
                autoSprint.onAttack();
                TriggerBotMod.getModuleManager().onAttackAll();
            }
            return; // One action per tick
        }

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
        boolean sprinting = mc.player.isSprinting();
        boolean hasMovement = (velX * velX + velZ * velZ) > 0.001;
        boolean falling = (velY <= -0.1) || (wasAirborne && lastVelY <= -0.1);

        wasAirborne = airborne;
        lastVelY = velY;

        if (sprinting) {
            sprintTicks++;
        } else {
            sprintTicks = 0;
        }

        if (ascending) return;
        if (onGround && !sprinting) return;
        if (onGround && sprintTicks < 1) return;
        if (onGround && !hasMovement) return;
        if (airborne && !falling) return;

        long currentTick = mc.world.getTime();
        if (currentTick == lastProcessedTick) return;
        lastProcessedTick = currentTick;

        if (cooldownTicks > 0) {
            cooldownTicks--;
            return;
        }

        if (mc.player.getAttackCooldownProgress(1.0f) < 0.85f) return;

        Entity target = findTarget(mc);
        if (target == null) return;

        if (target instanceof PlayerEntity pe && pe.isBlocking()
                && CombatUtil.isFacingUs(mc, target) && !autoStun.isEnabled()) {
            autoStun.onEnable();
            cooldownTicks = 1;
            return;
        }

        if (!target.isAlive() || target.isRemoved() || !CombatUtil.isInReach(mc, target)) return;

        if (airborne) {
            // Crits: fire immediately, no delay
            mc.interactionManager.attackEntity(mc.player, target);
            mc.player.swingHand(Hand.MAIN_HAND);
            cooldownTicks = 1;
            autoSprint.onAttack();
            TriggerBotMod.getModuleManager().onAttackAll();
        } else {
            // Ground attacks: queue for next tick to let movement settle
            pendingGroundAttack = true;
            queuedGroundTarget = target;
        }
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
