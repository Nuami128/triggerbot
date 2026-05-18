package com.example.triggerbot.module.impl;

import com.example.triggerbot.module.ClientModule;
import com.example.triggerbot.module.ModuleManager;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.AxeItem;
import net.minecraft.item.ItemStack;
import net.minecraft.item.SwordItem;
import net.minecraft.network.packet.c2s.play.UpdateSelectedSlotC2SPacket;
import net.minecraft.util.Hand;

import java.util.ArrayDeque;
import java.util.Deque;

/**
 * AutoStunModule — Minecraft 1.21.11 (Fabric)
 *
 * Automates the shield-break / axe-stun sequence:
 *   • Target shielding & facing player → axe swap → attack ×2 → sword swap back
 *   • Target not shielding → double axe attack
 *
 * All actions are queued one per tick to avoid multi-action flags.
 */
public class AutoStunModule implements ClientModule {

    private static final String MODULE_NAME = "AutoStun";

    private boolean enabled = false;
    private final Deque<Runnable> actionQueue = new ArrayDeque<>();
    private int previousSlot = 0;

    @Override
    public String getName() {
        return MODULE_NAME;
    }

    @Override
    public void onEnable() {
        enabled = true;
        actionQueue.clear();
        sendHotbarMessage("Auto Stun Enabled");
    }

    @Override
    public void onDisable() {
        enabled = false;
        actionQueue.clear();
        sendHotbarMessage("Auto Stun Disabled");
    }

    @Override
    public void onTick() {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (!enabled || mc.player == null || mc.world == null) return;

        // 1. Execute one queued action per tick
        if (!actionQueue.isEmpty()) {
            actionQueue.poll().run();
            return;
        }

        ClientPlayerEntity player = mc.player;

        // Preconditions
        if (!player.isOnGround()) return;
        if (player.getVelocity().y > 0) return;
        if (!player.isSprinting()) return;

        // Find nearest target
        TargetInfo target = findNearestShieldTarget(mc, player);
        if (target == null) return;

        // Choose sequence
        if (target.isShielding && target.isFacingPlayer) {
            queueShieldBreakSequence(player);
        } else {
            queueDoubleAxeSequence(player);
        }
    }

    // ── Sequences ────────────────────────────────
    private void queueShieldBreakSequence(ClientPlayerEntity player) {
        int axeSlot = findHotbarSlot(player.getInventory(), AxeItem.class);
        if (axeSlot == -1) return;

        previousSlot = player.getInventory().selectedSlot();

        actionQueue.add(() -> swapToSlot(player, axeSlot));
        actionQueue.add(() -> performCriticalAttack(player));
        actionQueue.add(() -> performCriticalAttack(player));
        actionQueue.add(() -> swapToSlot(player, previousSlot));
    }

    private void queueDoubleAxeSequence(ClientPlayerEntity player) {
        int axeSlot = findHotbarSlot(player.getInventory(), AxeItem.class);
        if (axeSlot == -1) return;

        previousSlot = player.getInventory().selectedSlot();

        actionQueue.add(() -> swapToSlot(player, axeSlot));
        actionQueue.add(() -> performCriticalAttack(player));
        actionQueue.add(() -> performCriticalAttack(player));
        actionQueue.add(() -> swapToSlot(player, previousSlot));
    }

    // ── Actions ────────────────────────────────
    private void swapToSlot(ClientPlayerEntity player, int slot) {
        player.getInventory().setSelectedSlot(slot);
        MinecraftClient.getInstance().getNetworkHandler().sendPacket(new UpdateSelectedSlotC2SPacket(slot));
    }

    private void performCriticalAttack(ClientPlayerEntity player) {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.targetedEntity == null || mc.interactionManager == null) return;
        float cooldown = player.getAttackCooldownProgress(0.5f);
        if (cooldown < 0.9f) return;

        mc.interactionManager.attackEntity(player, mc.targetedEntity);
        player.swingHand(Hand.MAIN_HAND);
    }

    // ── Target detection ────────────────────────────────
    private TargetInfo findNearestShieldTarget(MinecraftClient mc, ClientPlayerEntity player) {
        net.minecraft.entity.LivingEntity closest = null;
        double closestDist = Double.MAX_VALUE;

        for (var entity : mc.world.getEntities()) {
            if (!(entity instanceof net.minecraft.entity.player.PlayerEntity living)) continue;
            if (living == player) continue;

            double dist = player.squaredDistanceTo(living);
            if (dist > 16.0) continue;

            if (dist < closestDist) {
                closestDist = dist;
                closest = living;
            }
        }

        if (closest == null) return null;

        boolean shielding = closest.isBlocking();
        boolean facingPlayer = isFacingPlayer(closest, player);
        return new TargetInfo(closest, shielding, facingPlayer);
    }

    private boolean isFacingPlayer(net.minecraft.entity.LivingEntity target, ClientPlayerEntity player) {
        double dx = player.getX() - target.getX();
        double dz = player.getZ() - target.getZ();
        double angleToPlayer = Math.toDegrees(Math.atan2(-dx, dz));

        float targetYaw = ((target.getYaw() % 360) + 360) % 360;
        double normAngle = (angleToPlayer % 360 + 360) % 360;

        double diff = Math.abs(targetYaw - normAngle);
        if (diff > 180) diff = 360 - diff;
        return diff < 90.0;
    }

    // ── Inventory helpers ────────────────────────────────
    private <T> int findHotbarSlot(PlayerInventory inv, Class<T> itemClass) {
        for (int i = 0; i < 9; i++) {
            ItemStack stack = inv.getStack(i);
            if (!stack.isEmpty() && itemClass.isInstance(stack.getItem())) {
                return i;
            }
        }
        return -1;
    }

    // ── UI ────────────────────────────────
    private void sendHotbarMessage(String message) {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player == null) return;
        mc.player.sendMessage(
                net.minecraft.text.Text.literal("[AutoStun] " + message),
                true
        );
    }

    // ── Data ────────────────────────────────
    private static final class TargetInfo {
        final net.minecraft.entity.LivingEntity entity;
        final boolean isShielding;
        final boolean isFacingPlayer;

        TargetInfo(net.minecraft.entity.LivingEntity entity, boolean shielding, boolean facingPlayer) {
            this.entity = entity;
            this.isShielding = shielding;
            this.isFacingPlayer = facingPlayer;
        }
    }
}
