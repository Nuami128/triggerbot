package com.example.triggerbot.module.impl;

import com.example.triggerbot.module.ClientModule;
import com.example.triggerbot.util.CombatUtil;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.AxeItem;
import net.minecraft.util.Hand;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;

import java.util.Optional;

public class AutoStunModule implements ClientModule {

    private enum State { IDLE, WAITING, ATTACKING, SWAPPING_BACK, COOLDOWN }

    private boolean enabled = false;
    private State state = State.IDLE;
    private int originalSlot = -1;
    private int tickCounter = 0;
    private Entity cachedTarget = null;
    private long lastProcessedTick = -1L;

    @Override
    public String getName() { return "AutoStun"; }

    public boolean isEnabled() { return enabled; }

    @Override
    public void onEnable() {
        enabled = true;
        state = State.IDLE;
        tickCounter = 0;
        cachedTarget = null;
    }

    @Override
    public void onDisable() {
        enabled = false;
        state = State.IDLE;
        tickCounter = 0;
        cachedTarget = null;
        lastProcessedTick = -1L;
    }

    public void beginSwapBack() {
        if (state == State.ATTACKING || state == State.WAITING || state == State.IDLE) {
            tickCounter = 0;
            state = State.SWAPPING_BACK;
        } else if (state != State.SWAPPING_BACK) {
            onDisable();
        }
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

        // Don't fire while eating or shielding
        if (CombatUtil.isPlayerBusy(mc)) return;

        long currentTick = mc.world.getTime();
        if (currentTick == lastProcessedTick) return;
        lastProcessedTick = currentTick;

        tickCounter++;

        switch (state) {

            case IDLE -> {
                if (mc.player.getAttackCooldownProgress(1.0f) < 1.0f) return;

                // Only target players who are actively shielding and facing us
                Entity target = findShieldingTarget(mc);
                if (target == null) return;

                int axeSlot = findAxe(mc);
                if (axeSlot == -1) return;

                cachedTarget = target;
                originalSlot = mc.player.getInventory().getSelectedSlot();

                if (originalSlot != axeSlot) {
                    mc.player.getInventory().setSelectedSlot(axeSlot);
                }

                tickCounter = 0;
                state = State.WAITING;
            }

            case WAITING -> {
                if (tickCounter < 1) return;

                // Re-validate — target must still be shielding and facing us
                if (cachedTarget == null || !cachedTarget.isAlive() || cachedTarget.isRemoved()) {
                    tickCounter = 0;
                    state = State.SWAPPING_BACK;
                    return;
                }

                if (cachedTarget instanceof LivingEntity le && !CombatUtil.isShielding(le)) {
                    tickCounter = 0;
                    state = State.SWAPPING_BACK;
                    return;
                }

                if (!CombatUtil.isFacingUs(mc, cachedTarget)) {
                    tickCounter = 0;
                    state = State.SWAPPING_BACK;
                    return;
                }

                mc.interactionManager.attackEntity(mc.player, cachedTarget);
                mc.player.swingHand(Hand.MAIN_HAND);

                tickCounter = 0;
                state = State.ATTACKING;
            }

            case ATTACKING -> {
                if (tickCounter < 1) return;
                tickCounter = 0;
                state = State.SWAPPING_BACK;
            }

            case SWAPPING_BACK -> {
                if (tickCounter < 2) return;

                int current = mc.player.getInventory().getSelectedSlot();
                if (originalSlot != -1 && current != originalSlot) {
                    mc.player.getInventory().setSelectedSlot(originalSlot);
                }

                tickCounter = 0;
                state = State.COOLDOWN;
            }

            case COOLDOWN -> {
                if (tickCounter >= 10) {
                    enabled = false;
                    cachedTarget = null;
                    state = State.IDLE;
                }
            }
        }
    }

    // Only returns a target that is shielding AND facing us AND in reach
    private Entity findShieldingTarget(MinecraftClient mc) {
        Vec3d eyePos = mc.player.getEyePos();
        Vec3d look = mc.player.getRotationVec(1.0f);
        Vec3d reachVec = eyePos.add(look.multiply(3.0));

        for (Entity e : mc.world.getEntities()) {
            if (!(e instanceof PlayerEntity pe)) continue;
            if (e == mc.player) continue;
            if (!e.isAlive()) continue;
            if (e.isRemoved()) continue;
            if (e.isSpectator()) continue;

            // Must be in reach
            if (!CombatUtil.isInReach(mc, e)) continue;

            // Must be actively shielding
            if (!pe.isBlocking()) continue;

            // Must be facing us (shield facing toward us)
            if (!CombatUtil.isFacingUs(mc, e)) continue;

            Box box = e.getBoundingBox();
            Optional<Vec3d> hit = box.raycast(eyePos, reachVec);
            if (hit.isPresent()) return e;
        }
        return null;
    }

    private int findAxe(MinecraftClient mc) {
        for (int i = 0; i < 9; i++) {
            if (mc.player.getInventory().getStack(i).getItem() instanceof AxeItem)
                return i;
        }
        return -1;
    }
}
