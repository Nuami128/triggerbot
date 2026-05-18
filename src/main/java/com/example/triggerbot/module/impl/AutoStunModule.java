// Minecraft 1.21.11 (Fabric)
package com.example.triggerbot.module.impl;

import com.example.triggerbot.module.ClientModule;
import com.example.triggerbot.module.ModuleManager;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.AxeItem;
import net.minecraft.item.ItemStack;
import net.minecraft.network.packet.c2s.play.UpdateSelectedSlotC2SPacket;
import net.minecraft.util.Hand;
import org.lwjgl.glfw.GLFW;

import java.lang.reflect.Field;
import java.util.ArrayDeque;
import java.util.Deque;

public class AutoStunModule implements ClientModule {

    private static final String MODULE_NAME = "AutoStun";

    // ── Timing constants ──────────────────────────────────────────────────────
    /** Delay after a hotbar swap before the next action fires (makes swap visible). */
    private static final long SWAP_DELAY_MS   = 50L;

    /**
     * Delay between hit 1 (shield disable) and hit 2 (knockback stun).
     * 50 ms gives the server time to register the shield-disable effect
     * before the second hit lands, while still being within the stun window.
     */
    private static final long HIT_GAP_MS      = 50L;

    // ── Keybind ───────────────────────────────────────────────────────────────
    // Register via KeyBindingHelper.registerKeyBinding(KEYBIND) in your mod init.
    public static final KeyBinding KEYBIND = new KeyBinding(
            "key.triggerbot.autostun",
            InputUtil.Type.KEYSYM,
            GLFW.GLFW_KEY_R,
            "category.triggerbot"
    );

    // ── State ─────────────────────────────────────────────────────────────────
    private boolean enabled       = false;
    private int     previousSlot  = 0;

    /**
     * Action queue — drained one entry per tick, subject to the delay gate below.
     * This preserves legit-looking input spacing and avoids multi-action flags.
     */
    private final Deque<Runnable> actionQueue = new ArrayDeque<>();

    /**
     * Wall-clock timestamp of the last swap or hit action.
     * The queue will not drain while {@code System.currentTimeMillis() - lastActionTime < requiredDelay}.
     */
    private long lastActionTime   = 0L;
    private long requiredDelay    = 0L;

    // ── ClientModule impl ─────────────────────────────────────────────────────

    @Override
    public String getName() { return MODULE_NAME; }

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

    public boolean isEnabled() {
    return enabled;
    }
    
    @Override
    public void onTick() {
        // ── Drain one queued action, respecting the inter-action delay ─────────
        if (!actionQueue.isEmpty()) {
            long now = System.currentTimeMillis();
            if (now - lastActionTime < requiredDelay) return; // still waiting
            actionQueue.poll().run();
            return; // one action per tick
        }

        // ── Preconditions ─────────────────────────────────────────────────────
        if (!enabled) return;

        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player == null || mc.world == null) return;

        ClientPlayerEntity player = mc.player;

        if (!player.isOnGround())       return; // must be grounded
        if (player.getVelocity().y > 0) return; // must not be ascending
        // Sprinting is NOT required — crits are fall-state only, not sprint-state.

        // ── Find target ───────────────────────────────────────────────────────
        TargetInfo target = findNearestShieldTarget(mc, player);
        if (target == null) return;

        // ── Queue appropriate sequence ────────────────────────────────────────
        if (target.isShielding && target.isFacingPlayer) {
            queueShieldBreakStunSequence(mc, player);
        } else {
            queueDoubleAxeSequence(mc, player);
        }
    }

    // ── Sequences ─────────────────────────────────────────────────────────────

    /**
     * Shield-break + stun sequence.
     *
     * Timeline:
     *   t+0ms   : swap hotbar → axe           (visible swap, 50ms gate)
     *   t+50ms  : left-click #1 (axe hit)     → disables shield via vanilla mechanic
     *   t+100ms : left-click #2 (axe hit)     → lands in stun window, applies knockback
     *   t+150ms : swap hotbar → previous slot  (visible swap)
     *
     * The 50ms gap between hits matches the SWAP_DELAY_MS and gives the server
     * time to apply the shield-disable effect before hit #2 registers.
     * Both hits use the axe so axe-knockback is applied on hit #2.
     */
    private void queueShieldBreakStunSequence(MinecraftClient mc, ClientPlayerEntity player) {
        int axeSlot = findHotbarSlot(player.getInventory(), AxeItem.class);
        if (axeSlot == -1) return;

        previousSlot = getSelectedSlot(player.getInventory());

        // Step 1 — swap to axe, impose SWAP_DELAY before next action
        actionQueue.add(() -> {
            swapToSlot(mc, axeSlot);
            setDelay(SWAP_DELAY_MS);
            sendHotbarMessage("Shield Break — Axe Equipped");
        });

        // Step 2 — hit #1: disables the shield (vanilla: axe vs blocking = shield disable)
        actionQueue.add(() -> {
            performAttack(mc);
            setDelay(HIT_GAP_MS); // wait for shield-disable to register server-side
        });

        // Step 3 — hit #2: lands in the stun window, applies axe knockback
        actionQueue.add(() -> {
            performAttack(mc);
            setDelay(SWAP_DELAY_MS);
        });

        // Step 4 — swap back (no message)
        actionQueue.add(() -> {
            swapToSlot(mc, previousSlot);
            setDelay(SWAP_DELAY_MS);
        });
    }

    /**
     * Double-axe sequence when target is not actively shielding.
     *
     * Timeline:
     *   t+0ms   : swap → axe     (50ms gate)
     *   t+50ms  : left-click #1
     *   t+100ms : left-click #2
     *   t+150ms : swap → previous slot
     */
    private void queueDoubleAxeSequence(MinecraftClient mc, ClientPlayerEntity player) {
        int axeSlot = findHotbarSlot(player.getInventory(), AxeItem.class);
        if (axeSlot == -1) return;

        previousSlot = getSelectedSlot(player.getInventory());

        actionQueue.add(() -> {
            swapToSlot(mc, axeSlot);
            setDelay(SWAP_DELAY_MS);
        });
        actionQueue.add(() -> {
            performAttack(mc);
            setDelay(HIT_GAP_MS);
        });
        actionQueue.add(() -> {
            performAttack(mc);
            setDelay(SWAP_DELAY_MS);
        });
        actionQueue.add(() -> {
            swapToSlot(mc, previousSlot);
            setDelay(SWAP_DELAY_MS);
        });
    }

    // ── Atomic actions ────────────────────────────────────────────────────────

    /**
     * Sends a hotbar slot-change to the server and updates the client inventory.
     * The caller sets the required delay via {@link #setDelay(long)}.
     */
    private void swapToSlot(MinecraftClient mc, int slot) {
        if (mc.player == null) return;
        setSelectedSlot(mc.player.getInventory(), slot);
        mc.getNetworkHandler().sendPacket(new UpdateSelectedSlotC2SPacket(slot));
    }

    /**
     * Performs a single attack (left-click) on the currently crosshair-targeted entity.
     *
     * Critical hit fires naturally when:
     *   • player.getVelocity().y < 0  (falling)
     *   • not on ladder / in liquid
     *   • cooldown ≥ 0.9
     *
     * No velocity injection — crits are purely condition-gated.
     * Sprinting is not required.
     */
    private void performAttack(MinecraftClient mc) {
        if (mc.player == null || mc.interactionManager == null) return;
        if (mc.targetedEntity == null) return;

        float cooldown = mc.player.getAttackCooldownProgress(0.5f);
        if (cooldown < 0.9f) return;

        mc.interactionManager.attackEntity(mc.player, mc.targetedEntity);
        mc.player.swingHand(Hand.MAIN_HAND);
    }

    // ── Delay helpers ─────────────────────────────────────────────────────────

    /**
     * Records the current wall-clock time and the minimum delay before the
     * next queued action may fire. Called immediately after each action runs.
     */
    private void setDelay(long delayMs) {
        lastActionTime = System.currentTimeMillis();
        requiredDelay  = delayMs;
    }

    // ── Target detection ──────────────────────────────────────────────────────

    private TargetInfo findNearestShieldTarget(MinecraftClient mc,
                                               ClientPlayerEntity player) {
        if (mc.world == null) return null;

        net.minecraft.entity.LivingEntity closest    = null;
        double                             closestDist = Double.MAX_VALUE;

        for (net.minecraft.entity.Entity entity : mc.world.getEntities()) {
            if (!(entity instanceof net.minecraft.entity.LivingEntity living)) continue;
            if (living == player) continue;
            if (!(living instanceof net.minecraft.entity.player.PlayerEntity)) continue;

            double dist = player.squaredDistanceTo(living);
            if (dist > 16.0) continue; // 4-block radius (4² = 16)

            if (dist < closestDist) {
                closestDist = dist;
                closest     = living;
            }
        }

        if (closest == null) return null;

        return new TargetInfo(closest, closest.isBlocking(), isFacingPlayer(closest, player));
    }

    private boolean isFacingPlayer(net.minecraft.entity.LivingEntity target,
                                    ClientPlayerEntity player) {
        double dx           = player.getX() - target.getX();
        double dz           = player.getZ() - target.getZ();
        double angleToPlayer = Math.toDegrees(Math.atan2(-dx, dz));

        float  targetYaw  = ((target.getYaw() % 360) + 360) % 360;
        double normAngle  = (angleToPlayer    % 360  + 360) % 360;

        double diff = Math.abs(targetYaw - normAngle);
        if (diff > 180) diff = 360 - diff;

        return diff < 90.0;
    }

    // ── Inventory helpers ─────────────────────────────────────────────────────

    private <T> int findHotbarSlot(PlayerInventory inv, Class<T> itemClass) {
        for (int i = 0; i < 9; i++) {
            ItemStack stack = inv.getStack(i);
            if (!stack.isEmpty() && itemClass.isInstance(stack.getItem())) return i;
        }
        return -1;
    }

    // ── UI helpers ────────────────────────────────────────────────────────────

    private void sendHotbarMessage(String message) {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player == null) return;
        mc.player.sendMessage(
                net.minecraft.text.Text.literal("[AutoStun] " + message),
                true // hotbar overlay
        );
    }

    // ── Reflection helpers (selectedSlot) ─────────────────────────────────────

    private int getSelectedSlot(PlayerInventory inv) {
        try {
            Field f = PlayerInventory.class.getDeclaredField("selectedSlot");
            f.setAccessible(true);
            return f.getInt(inv);
        } catch (Exception e) {
            e.printStackTrace();
            return 0;
        }
    }

    private void setSelectedSlot(PlayerInventory inv, int slot) {
        try {
            Field f = PlayerInventory.class.getDeclaredField("selectedSlot");
            f.setAccessible(true);
            f.setInt(inv, slot);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // ── Inner data class ──────────────────────────────────────────────────────

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
