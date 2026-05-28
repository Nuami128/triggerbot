package com.example.triggerbot.module.impl;

import com.example.triggerbot.module.ClientModule;
import com.example.triggerbot.util.CombatUtil;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.AxeItem;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Hand;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;

import java.util.Optional;

public class AutoStunModule implements ClientModule {

    private enum State { IDLE, WAITING, SHIELD_BREAK, STUN_DELAY, STUN, SWAPPING_BACK, COOLDOWN }

    private boolean enabled = false;
    private State state = State.IDLE;
    private int originalSlot = -1;
    private int tickCounter = 0;
    private Entity cachedTarget = null;
    private long lastProcessedTick = -1L;

    private boolean wasBlocking = false;
    private boolean wasUsingItem = false;

    @Override
    public String getName() { return "AutoStun"; }

    public boolean isEnabled() { return enabled; }

    @Override
    public void onEnable() {
        enabled = true;
        state = State.IDLE;
        tickCounter = 0;
        cachedTarget = null;
        wasBlocking = false;
        wasUsingItem = false;
    }

    @Override
    public void onDisable() {
        enabled = false;
        state = State.IDLE;
        tickCounter = 0;
        cachedTarget = null;
        lastProcessedTick = -1L;
        wasBlocking = false;
        wasUsingItem = false;
    }

    public void beginSwapBack() {
        if (state != State.SWAPPING_BACK && state != State.COOLDOWN) {
            tickCounter = 0;
            state = State.SWAPPING_BACK;
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

        if (CombatUtil.isPlayerBusy(mc)) return;
        if (mc.player.getVelocity().y > 0) return;

        // Guard: skip the tick the player releases their own item use
        boolean playerUsingItem = mc.player.isUsingItem();
        boolean justReleasedItem = wasUsingItem && !playerUsingItem;
        wasUsingItem = playerUsingItem;
        if (justReleasedItem) return;

        long currentTick = mc.world.getTime();
        if (currentTick == lastProcessedTick) return;
        lastProcessedTick = currentTick;

        // Guard: skip the tick the target stops blocking
        boolean currentlyBlocking = cachedTarget instanceof PlayerEntity pe && pe.isBlocking();
        boolean justStoppedBlocking = wasBlocking && !currentlyBlocking;
        wasBlocking = currentlyBlocking;

        tickCounter++;

        switch (state) {

            case IDLE -> {
                if (mc.player.getAttackCooldownProgress(1.0f) < 1.0f) return;

                ItemStack held = mc.player.getMainHandStack();
                if (!CombatUtil.isSword(held) && !CombatUtil.isAxe(held)) {
                    onDisable();
                    return;
                }

                Entity target = findShieldingTarget(mc);
                if (target == null) {
                    onDisable();
                    return;
                }

                int axeSlot = findAxe(mc);
                if (axeSlot == -1) {
                    onDisable();
                    return;
                }

                int currentSlot = mc.player.getInventory().getSelectedSlot();
                ItemStack currentItem = mc.player.getInventory().getStack(currentSlot);
                if (!CombatUtil.isSword(currentItem)) {
                    onDisable();
                    return;
                }

                cachedTarget = target;
                originalSlot = currentSlot;
                wasBlocking = target instanceof PlayerEntity pe && pe.isBlocking();

                if (originalSlot != axeSlot) {
                    mc.player.getInventory().setSelectedSlot(axeSlot);
                }

                tickCounter = 0;
                state = State.WAITING;
            }

            case WAITING -> {
                if (tickCounter < 1) return;
                if (!isTargetValid(mc)) {
                    tickCounter = 0;
                    state = State.SWAPPING_BACK;
                    return;
                }
                if (justStoppedBlocking) return;

                mc.interactionManager.attackEntity(mc.player, cachedTarget);
                mc.player.swingHand(Hand.MAIN_HAND);

                tickCounter = 0;
                state = State.SHIELD_BREAK;
            }

            case SHIELD_BREAK -> {
                if (tickCounter < 1) return;
                tickCounter = 0;
                state = State.STUN_DELAY;
            }

            case STUN_DELAY -> {
                if (tickCounter < 1) return;
                tickCounter = 0;
                state = State.STUN;
            }

            case STUN -> {
                if (!isTargetValid(mc)) {
                    tickCounter = 0;
                    state = State.SWAPPING_BACK;
                    return;
                }
                if (justStoppedBlocking) return;

                mc.interactionManager.attackEntity(mc.player, cachedTarget);
                mc.player.swingHand(Hand.MAIN_HAND);

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
                    onDisable();
                }
            }
        }
    }

    private boolean isTargetValid(MinecraftClient mc) {
        if (cachedTarget == null) return false;
        if (!cachedTarget.isAlive()) return false;
        if (cachedTarget.isRemoved()) return false;
        if (!CombatUtil.isInReach(mc, cachedTarget)) return false;
        return true;
    }

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
            if (!CombatUtil.isInReach(mc, e)) continue;
            if (!pe.isBlocking()) continue;
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
