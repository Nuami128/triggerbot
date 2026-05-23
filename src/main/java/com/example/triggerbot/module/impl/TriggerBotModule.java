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

    public TriggerBotModule(AutoStunModule autoStun) {
        this.autoStun = autoStun;
    }

    @Override
    public String getName() { return "TriggerBot"; }

    public boolean isEnabled() { return enabled; }

    @Override
    public void onEnable() { enabled = true; }

    @Override
    public void onDisable() {
        enabled = false;
        cooldownTicks = 0;
        lastProcessedTick = -1L;
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

        if (CombatUtil.isPlayerBusy(mc)) return;

        // Only fire with sword or axe
        ItemStack held = mc.player.getMainHandStack();
        if (!CombatUtil.isSword(held) && !CombatUtil.isAxe(held)) return;

        double velY = mc.player.getVelocity().y;
        boolean onGround = mc.player.isOnGround();
        boolean ascending = velY > 0;
        boolean airborne = !onGround;
        boolean sprinting = mc.player.isSprinting();

        // Don't attack while ascending
        if (ascending) return;

        // Must be sprinting on ground, OR airborne for crits
        if (onGround && !sprinting) return;

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
