package com.example.triggerbot.module.impl;

import com.example.triggerbot.module.ClientModule;
import com.example.triggerbot.util.CombatUtil;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.Hand;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;

import java.util.Optional;

public class TriggerBotModule implements ClientModule {

    private boolean enabled = false;
    private long lastProcessedTick = -1L;
    private int cooldownTicks = 0;

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

        // Don't attack while eating or shielding
        if (CombatUtil.isPlayerBusy(mc)) return;

        long currentTick = mc.world.getTime();
        if (currentTick == lastProcessedTick) return;
        lastProcessedTick = currentTick;

        if (cooldownTicks > 0) {
            cooldownTicks--;
            return;
        }

        if (mc.player.getAttackCooldownProgress(1.0f) < 1.0f) return;

        Entity target = findTarget(mc);
        if (target == null) return;

        mc.interactionManager.attackEntity(mc.player, target);
        mc.player.swingHand(Hand.MAIN_HAND);

        cooldownTicks = 10;
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

            // Skip shielding players — AutoStun handles those
            if (e instanceof PlayerEntity pe && pe.isBlocking()) continue;

            // Max 3 block reach
            if (!CombatUtil.isInReach(mc, e)) continue;

            Box box = e.getBoundingBox();
            Optional<Vec3d> hit = box.raycast(eyePos, reachVec);
            if (hit.isPresent()) return e;
        }
        return null;
    }
}
