// Minecraft 1.21.11 (Fabric)
package com.example.triggerbot.module.impl;

import com.example.triggerbot.module.ClientModule;
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

import java.util.ArrayDeque;
import java.util.Deque;

public class AutoStunModule implements ClientModule {

    private static final String MODULE_NAME = "AutoStun";

    // ── Timing ────────────────────────────────────────────────────────────────
    /** Delay after each hotbar slot step — makes the swap visible, prevents skipping. */
    private static final long SWAP_DELAY_MS = 50L;

    /** Delay between hit #1 (shield disable) and hit #2 (knockback stun). */
    private static final long HIT_GAP_MS = 50L;

    // ── Keybind ───────────────────────────────────────────────────────────────
    // Register via KeyBindingHelper.registerKeyBinding(KEYBIND) in your mod init.
    // Poll it each tick via: if (KEYBIND.wasPressed()) module.toggle();
    public static final KeyBinding KEYBIND = new KeyBinding(
            "key.triggerbot.autostun",
            InputUtil.Type.KEYSYM,
            GLFW.GLFW_KEY_R,
            "category.triggerbot.modules"
    );

    // ── State ─────────────────────────────────────────────────────────────────
    private boolean enabled      = false;
    private int     previousSlot = 0;

    /** One action fires per tick, subject to the delay gate. */
    private final Deque<Runnable> actionQueue = new ArrayDeque<>();

    private long lastActionTime = 0L;
    private long requiredDelay  = 0L;

    // ── Public toggle (called by keybind listener in mod init) ────────────────

    public void toggle() {
        if (enabled) onDisable();
        else onEnable();
    }

    public boolean isEnabled() {
        return enabled;
    }

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

    @Override
    public void onTick() {
        // ── Drain one queued action, respecting inter-action delay ─────────────
        if (!actionQueue.isEmpty()) {
            if (System.currentTimeMillis() - lastActionTime < requiredDelay) return;
            actionQueue.poll().run();
            return;
        }

        // ── Preconditions ─────────────────────────────────────────────────────
        if (!enabled) return;

        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player == null || mc.world == null) return;

        ClientPlayerEntity player = mc.player;

        if (!player.isOnGround())       return; // must be grounded
        if (player.getVelocity().y > 0) return; // must not be ascending

        // ── Find a target that is ACTIVELY SHIELDING ──────────────────────────
        // We only act when a nearby player has their shield up.
        // This prevents acting as a generic autoclicker on shieldless targets.
        TargetInfo target = findActivelyShieldingTarget(mc, player);
        if (target == null) return;

        // Only run the sequence if target is shielding AND facing us
        if (!target.isShielding || !target.isFacingPlayer) return;

        queueShieldBreakStunSequence(mc, player);
    }

    // ── Sequence ──────────────────────────────────────────────────────────────

    /**
     * Shield-break + stun sequence:
     *
     *   t+0ms    swap step toward axe slot   (one slot per step, 50ms each — no skipping)
     *   t+50ms   ... repeat until axe slot reached
     *   t+Xms    left-click #1 (axe)         → shield disabled by vanilla mechanic
     *   t+X+50ms left-click #2 (axe)         → lands in stun window, applies knockback
     *   t+X+100ms swap step back toward previousSlot (one slot per step, 50ms each)
     */
    private void queueShieldBreakStunSequence(MinecraftClient mc, ClientPlayerEntity player) {
        int axeSlot = findHotbarSlot(player.getInventory(), AxeItem.class);
        if (axeSlot == -1) return; // no axe in hotbar — abort

        previousSlot = player.getInventory().selectedSlot;

        // If already on the axe slot, skip the swap steps
        if (previousSlot != axeSlot) {
            queueSteppedSwap(mc, previousSlot, axeSlot);
        }

        // Hit #1 — disables the shield
        actionQueue.add(() -> {
            performAttack(mc);
            setDelay(HIT_GAP_MS);
        });

        // Hit #2 — lands in stun window, axe knockback applied
        actionQueue.add(() -> {
            performAttack(mc);
            setDelay(SWAP_DELAY_MS);
        });

        // Swap back one step at a time
        if (previousSlot != axeSlot) {
            queueSteppedSwap(mc, axeSlot, previousSlot);
        }
    }

    /**
     * Queues individual one-slot steps from {@code fromSlot} to {@code toSlot}.
     * Each step moves exactly one position and waits {@code SWAP_DELAY_MS} before
     * the next — this makes every intermediate slot briefly visible in the hotbar
     * and eliminates the "hotbar skip" effect.
     */
    private void queueSteppedSwap(MinecraftClient mc, int fromSlot, int toSlot) {
        int direction = (toSlot > fromSlot) ? 1 : -1;
        int current   = fromSlot;

        while (current != toSlot) {
            current += direction;
            final int targetSlot = current;
            actionQueue.add(() -> {
                swapToSlot(mc, targetSlot);
                setDelay(SWAP_DELAY_MS);
            });
        }
    }

    // ── Atomic actions ────────────────────────────────────────────────────────

    /**
     * Steps the hotbar selection by exactly one slot and notifies the server.
     * Never jumps — called once per queued step.
     */
    private void swapToSlot(MinecraftClient mc, int slot) {
        if (mc.player == null) return;
        mc.player.getInventory().selectedSlot = slot;
        mc.getNetworkHandler().sendPacket(new UpdateSelectedSlotC2SPacket(slot));
    }

    /**
     * Fires one attack on the crosshair target if the cooldown allows it.
     * Crits trigger naturally when the player is falling — no sprint required,
     * no velocity manipulation applied.
     */
    private void performAttack(MinecraftClient mc) {
        if (mc.player == null || mc.interactionManager == null) return;
        if (mc.targetedEntity == null) return;

        float cooldown = mc.player.getAttackCooldownProgress(0.5f);
        if (cooldown < 0.9f) return;

        mc.interactionManager.attackEntity(mc.player, mc.targetedEntity);
        mc.player.swingHand(Hand.MAIN_HAND);
    }

    // ── Delay helper ──────────────────────────────────────────────────────────

    private void setDelay(long delayMs) {
        lastActionTime = System.currentTimeMillis();
        requiredDelay  = delayMs;
    }

    // ── Target detection ──────────────────────────────────────────────────────

    /**
     * Finds the nearest player within 4 blocks that is ACTIVELY BLOCKING
     * (shield up) AND facing us. Returns {@code null} if no such target exists.
     *
     * This is the key guard that prevents the module acting as an autoclicker —
     * it will not queue any actions unless a shield is actually raised.
     */
    private TargetInfo findActivelyShieldingTarget(MinecraftClient mc,
                                                    ClientPlayerEntity player) {
        if (mc.world == null) return null;

        net.minecraft.entity.LivingEntity closest     = null;
        double                             closestDist = Double.MAX_VALUE;

        for (net.minecraft.entity.Entity entity : mc.world.getEntities()) {
            if (!(entity instanceof net.minecraft.entity.LivingEntity living)) continue;
            if (living == player) continue;
            if (!(living instanceof net.minecraft.entity.player.PlayerEntity)) continue;

            // Only consider players who are actively blocking right now
            if (!living.isBlocking()) continue;

            double dist = player.squaredDistanceTo(living);
            if (dist > 16.0) continue; // 4-block radius (4² = 16)

            if (dist < closestDist) {
                closestDist = dist;
                closest     = living;
            }
        }

        if (closest == null) return null;

        return new TargetInfo(closest, true, isFacingPlayer(closest, player));
    }

    private boolean isFacingPlayer(net.minecraft.entity.LivingEntity target,
                                    ClientPlayerEntity player) {
        double dx            = player.getX() - target.getX();
        double dz            = player.getZ() - target.getZ();
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
                true
        );
    }
}
