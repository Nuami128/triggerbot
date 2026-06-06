package com.example.triggerbot.module.impl;

import com.example.triggerbot.TriggerBotMod;
import com.example.triggerbot.mixin.MinecraftClientAccessor;
import com.example.triggerbot.module.ClientModule;
import com.example.triggerbot.util.CombatUtil;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;

public class TriggerBotModule implements ClientModule {

    private final AutoStunModule autoStun;
    private final AutoSprintModule autoSprint;

    private boolean enabled = false;
    private long lastProcessedTick = -1L;
    private int cooldownTicks = 0;
    private int releaseDelay = 0;
    private int itemReleaseCooldown = 0;
    private boolean wasAirborne = false;
    private boolean wasSprintingLastTick = false;
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
        wasSprintingLastTick = false;
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
        wasSprintingLastTick = false;
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
        boolean sprinting = mc.player.isSprinting();
        boolean hasMovement = (velX * velX + velZ * velZ) > 0.001;
        boolean falling = (velY <= -0.1) || (wasAirborne && lastVelY <= -0.1);

        // Skip only the exact landing tick when we actually fell (lastVelY <= -0.1).
        // A step-up has lastVelY near 0 so it won't be skipped — no missed hits.
        // Grim's Simulation check snapshots velY on the landing tick; firing an
        // attack here desynchronises its physics prediction. One tick is enough.
        if (wasAirborne && onGround && lastVelY <= -0.1) {
            wasAirborne = airborne;
            wasSprintingLastTick = sprinting;
            lastVelY = velY;
            return;
        }

        // Ground sprint hit: fire on the tick sprint is released.
        // wasSprintingLastTick=true + sprinting=false means AutoSprint just
        // dropped the sprint key this tick. Grim sees: sprinting → sprint-break
        // → attack, which is physically identical to a real player's sprint hit.
        boolean groundSprintHit = onGround && hasMovement && wasSprintingLastTick && !sprinting;

        // Update state AFTER reading it for this tick's decision
        wasAirborne = airborne;
        wasSprintingLastTick = sprinting;
        lastVelY = velY;

        if (ascending) return;
        if (airborne && !falling) return;
        if (onGround && !groundSprintHit) return;

        long currentTick = mc.world.getTime();
        if (currentTick == lastProcessedTick) return;
        lastProcessedTick = currentTick;

        if (cooldownTicks > 0) {
            cooldownTicks--;
            return;
        }
        if (mc.player.getAttackCooldownProgress(1.0f) < 0.85f) return;

        Entity target = mc.targetedEntity;
        if (target == null) return;
        if (!(target instanceof LivingEntity)) return;
        if (!target.isAlive() || target.isRemoved()) return;
        if (!CombatUtil.isInReach(mc, target)) return;
        if (target instanceof PlayerEntity pe && pe.isBlocking()
                && CombatUtil.isFacingUs(mc, target) && !autoStun.isEnabled()) {
            autoStun.onEnable();
            cooldownTicks = 1;
            return;
        }

        ((MinecraftClientAccessor) mc).invokeDoAttack();
        cooldownTicks = 1;
        autoSprint.onAttack();
        TriggerBotMod.getModuleManager().onAttackAll();
    }
}
