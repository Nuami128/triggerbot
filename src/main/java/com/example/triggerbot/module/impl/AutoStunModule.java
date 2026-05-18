// Minecraft 1.21.11 (Fabric)
package com.example.triggerbot.module.impl;

import com.example.modules.api.ClientModule;
import com.example.modules.api.EmptyModule;
import com.example.modules.api.ModuleManager;
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
 *  • Target shielding & facing player → axe swap → attack ×2 → sword swap back
 *  • Target not shielding or already exposed  → double axe hit → done
 *
 * All actions are queued one per tick to avoid multi-action flags.
 * No aim-assist or movement manipulation is included.
 */
public class AutoStunModule implements ClientModule {

    // ── Constants ──────────────────────────────────────────────────────────────
    private static final String MODULE_NAME = "AutoStun";

    // ── State ──────────────────────────────────────────────────────────────────
    private boolean enabled = false;

    /** Tick-separated action queue so nothing fires in the same tick. */
    private final Deque<Runnable> actionQueue = new ArrayDeque<>();

    /** Slot we were on before swapping to axe, so we can restore it. */
    private int previousSlot = 0;

    // ── ClientModule impl ──────────────────────────────────────────────────────

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

    /**
     * Called every client tick by {@link ModuleManager}.
     * One queued action is drained per tick — this is the core
     * anti-flag mechanism.
     */
    @Override
    public void onTick() {
        // ── 1. Drain one queued action per tick ────────────────────────────────
        if (!actionQueue.isEmpty()) {
            actionQueue.poll().run();
            return; // only one action per tick
        }

        // ── 2. Preconditions ──────────────────────────────────────────────────
        if (!enabled) return;

        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player == null || mc.world == null) return;

        ClientPlayerEntity player = mc.player;

        if (!player.isOnGround())       return; // must be grounded
        if (player.getVelocity().y > 0) return; // must not be ascending
        if (!player.isSprinting())      return; // must be sprinting

        // ── 3. Find nearest valid target ──────────────────────────────────────
        TargetInfo target = findNearestShieldTarget(mc, player);
        if (target == null) return;

        // ── 4. Choose sequence based on target state ──────────────────────────
        if (target.isShielding && target.isFacingPlayer) {
            queueShieldBreakSequence(mc, player);
        } else {
            queueDoubleAxeSequence(mc, player);
        }
    }

    // ── Sequence builders ──────────────────────────────────────────────────────

    /**
     * Shield-break sequence:
     *   tick 0 → swap to axe
     *   tick 1 → critical attack
     *   tick 2 → critical attack
     *   tick 3 → swap back to sword
     */
    private void queueShieldBreakSequence(MinecraftClient mc, ClientPlayerEntity player) {
        int axeSlot = findHotbarSlot(player.getInventory(), AxeItem.class);
        if (axeSlot == -1) return; // no axe in hotbar

        previousSlot = player.getInventory().selectedSlot;

        actionQueue.add(() -> {
            swapToSlot(mc, axeSlot);
            sendHotbarMessage("Shield Break — Axe Equipped");
        });
        actionQueue.add(() -> performCriticalAttack(mc));
        actionQueue.add(() -> performCriticalAttack(mc));
        actionQueue.add(() -> {
            swapToSlot(mc, previousSlot);
            sendHotbarMessage("Shield Break Executed");
        });
    }

    /**
     * Double-axe sequence (target not actively shielding):
     *   tick 0 → swap to axe
     *   tick 1 → critical attack
     *   tick 2 → critical attack
     *   tick 3 → swap back
     */
    private void queueDoubleAxeSequence(MinecraftClient mc, ClientPlayerEntity player) {
        int axeSlot = findHotbarSlot(player.getInventory(), AxeItem.class);
        if (axeSlot == -1) return;

        previousSlot = player.getInventory().selectedSlot;

        actionQueue.add(() -> swapToSlot(mc, axeSlot));
        actionQueue.add(() -> performCriticalAttack(mc));
        actionQueue.add(() -> performCriticalAttack(mc));
        actionQueue.add(() -> swapToSlot(mc, previousSlot));
    }

    // ── Atomic actions ─────────────────────────────────────────────────────────

    /**
     * Switches the hotbar to {@code slot} and sends the required slot-update
     * packet so the server acknowledges the change.
     */
    private void swapToSlot(MinecraftClient mc, int slot) {
        if (mc.player == null) return;
        mc.player.getInventory().selectedSlot = slot;
        mc.getNetworkHandler().sendPacket(new UpdateSelectedSlotC2SPacket(slot));
    }

    /**
     * Performs a critical attack on the entity currently targeted by the
     * crosshair ({@code mc.targetedEntity}).
     *
     * A critical hit in vanilla requires:
     *   • Player is falling (velocity.y < 0)
     *   • Not on a ladder / in liquid
     *   • Attack cooldown is full (1.0)
     *
     * We only call this if the game's own conditions allow it — no forced
     * velocity manipulation is applied.
     */
    private void performCriticalAttack(MinecraftClient mc) {
        if (mc.player == null || mc.interactionManager == null) return;
        if (mc.targetedEntity == null) return;

        // Only attack when the attack cooldown is ready (>= 0.9 is the
        // vanilla threshold for a critical hit; 1.0 = fully charged).
        float cooldown = mc.player.getAttackCooldownProgress(0.5f);
        if (cooldown < 0.9f) return;

        mc.interactionManager.attackEntity(mc.player, mc.targetedEntity);
        mc.player.swingHand(Hand.MAIN_HAND);
    }

    // ── Target detection ───────────────────────────────────────────────────────

    /**
     * Scans nearby living entities (≤ 4 blocks) for valid stun targets.
     * Returns basic shield/facing state for the closest one,
     * or {@code null} if none qualifies.
     */
    private TargetInfo findNearestShieldTarget(MinecraftClient mc,
                                               ClientPlayerEntity player) {
        if (mc.world == null) return null;

        net.minecraft.entity.LivingEntity closest = null;
        double closestDist = Double.MAX_VALUE;

        for (net.minecraft.entity.Entity entity : mc.world.getEntities()) {
            if (!(entity instanceof net.minecraft.entity.LivingEntity living)) continue;
            if (living == player) continue;
            if (!(living instanceof net.minecraft.entity.player.PlayerEntity)) continue;

            double dist = player.squaredDistanceTo(living);
            if (dist > 16.0) continue; // 4-block radius (4² = 16)

            if (dist < closestDist) {
                closestDist = dist;
                closest = living;
            }
        }

        if (closest == null) return null;

        boolean shielding    = closest.isBlocking();
        boolean facingPlayer = isFacingPlayer(closest, player);

        return new TargetInfo(closest, shielding, facingPlayer);
    }

    /**
     * Returns {@code true} when {@code target}'s yaw points roughly toward
     * {@code player} (within ±90°).
     */
    private boolean isFacingPlayer(net.minecraft.entity.LivingEntity target,
                                    ClientPlayerEntity player) {
        double dx = player.getX() - target.getX();
        double dz = player.getZ() - target.getZ();
        double angleToPlayer = Math.toDegrees(Math.atan2(-dx, dz));

        float targetYaw  = ((target.getYaw() % 360) + 360) % 360;
        double normAngle = (angleToPlayer % 360 + 360) % 360;

        double diff = Math.abs(targetYaw - normAngle);
        if (diff > 180) diff = 360 - diff;

        return diff < 90.0;
    }

    // ── Inventory helpers ──────────────────────────────────────────────────────

    /**
     * Finds the first hotbar slot (0–8) holding an item of the given
     * {@code itemClass}, or {@code -1} if none is present.
     */
    private <T> int findHotbarSlot(PlayerInventory inv, Class<T> itemClass) {
        for (int i = 0; i < 9; i++) {
            ItemStack stack = inv.getStack(i);
            if (!stack.isEmpty() && itemClass.isInstance(stack.getItem())) {
                return i;
            }
        }
        return -1;
    }

    // ── UI helpers ─────────────────────────────────────────────────────────────

    /** Displays a transient message in the hotbar overlay. */
    private void sendHotbarMessage(String message) {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player == null) return;
        mc.player.sendMessage(
                net.minecraft.text.Text.literal("[AutoStun] " + message),
                true // true = overlay / hotbar
        );
    }

    // ── Inner data class ───────────────────────────────────────────────────────

    /** Lightweight snapshot of a target's relevant state at detection time. */
    private static final class TargetInfo {
        final net.minecraft.entity.LivingEntity entity;
        final boolean isShielding;
        final boolean isFacingPlayer;

        TargetInfo(net.minecraft.entity.LivingEntity entity,
                   boolean isShielding,
                   boolean isFacingPlayer) {
            this.entity         = entity;
            this.isShielding    = isShielding;
            this.isFacingPlayer = isFacingPlayer;
        }
    }
}
