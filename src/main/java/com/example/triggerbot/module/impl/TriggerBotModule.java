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

    private float lastHealth = -1f;
    private boolean recentlyHit = false;
    private int hitCooldown = 0;

    public TriggerBotModule(AutoStunModule autoStun) {
        this.autoStun = autoStun;
    }

    @Override
    public String getName() {
        return "TriggerBot";
    }

    public boolean isEnabled() {
        return enabled;
    }

    @Override
    public void onEnable() {
        enabled = true;
        lastHealth = -1f;
        recentlyHit = false;
        hitCooldown = 0;
    }

    @Override
    public void onDisable() {
        enabled = false;
        cooldownTicks = 0;
        lastProcessedTick = -1L;
        recentlyHit = false;
        hitCooldown = 0;
    }

    @Override
    public void onTick() {

        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player == null || mc.world == null) return;
        if (!enabled) return;

        // ---------------- DAMAGE TRACK ----------------
        float currentHealth = mc.player.getHealth();

        if (lastHealth > 0 && currentHealth < lastHealth) {
            recentlyHit = true;
            hitCooldown = 12;
        }

        lastHealth = currentHealth;

        if (hitCooldown > 0) hitCooldown--;
        else recentlyHit = false;

        // ---------------- BASIC CHECKS ----------------
        if (mc.currentScreen != null) return;
        if (mc.player.isDead()) return;
        if (mc.interactionManager == null) return;
        if (mc.getNetworkHandler() == null) return;

        ItemStack held = mc.player.getMainHandStack();
        if (!CombatUtil.isSword(held) && !CombatUtil.isAxe(held)) return;

        // cooldown
        if (cooldownTicks > 0) {
            cooldownTicks--;
            return;
        }

        if (mc.player.getAttackCooldownProgress(1.0f) < 0.85f) return;

        Entity target = findTarget(mc);
        if (target == null) return;

        // ---------------- AUTO STUN ----------------
        if (target instanceof PlayerEntity pe
                && pe.isBlocking()
                && CombatUtil.isFacingUs(mc, target)
                && !autoStun.isEnabled()) {

            autoStun.onEnable();
            cooldownTicks = 1;
            return;
        }

        // ---------------- ATTACK ----------------
        mc.interactionManager.attackEntity(mc.player, target);
        mc.player.swingHand(Hand.MAIN_HAND);

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

            if (hit.isPresent()) return e;
        }

        return null;
    }
}
