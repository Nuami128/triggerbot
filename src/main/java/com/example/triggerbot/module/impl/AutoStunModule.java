package com.example.triggerbot.module.impl;

import com.example.triggerbot.module.EmptyModule;
import java.util.ArrayDeque;
import java.util.HashMap;
import java.util.Map;
import java.util.Queue;
import java.util.Random;
import java.util.UUID;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.registry.tag.ItemTags;
import net.minecraft.text.Text;
import net.minecraft.util.Hand;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Vec3d;
import org.lwjgl.glfw.GLFW;

public class AutoStunModule extends EmptyModule {
    private static final float MIN_ATTACK_COOLDOWN = 0.9f;
    private static final float MAX_ATTACK_RANGE = 2.95f;
    private static final long BASE_SHIELD_DELAY_MS = 50L;
    private static final int HOTBAR_START_SLOT = 0;
    private static final int HOTBAR_SLOT_COUNT = 9;
    private static final int MIN_ACTION_DELAY_TICKS = 1;
    private static final int RANDOM_EXTRA_DELAY_TICKS = 1;
    private static final long DEBUG_TICK_INTERVAL = 20L;
    private static final boolean DEBUG_LOGGING = true;
    private static final KeyBinding.Category TRIGGERBOT_CATEGORY = KeyBinding.Category.create(
            Identifier.of("triggerbot", "triggerbot")
    );

    private final Map<UUID, Long> shieldStartTimes = new HashMap<>();
    private final Queue<StunAction> actionQueue = new ArrayDeque<>();
    private final Random random = new Random();

    private KeyBinding toggleKey;
    private StunSequence activeSequence;
    private boolean enabled = true;
    private int actionDelayTicks;
    private long tickCounter;
    private long lastAttackTick = -1L;
    private long lastDebugTick = -1L;

    public AutoStunModule() {
        super("Auto Stun");
    }

    @Override
    public void onInitialize() {
        toggleKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.triggerbot.auto_stun",
                InputUtil.Type.KEYSYM,
                GLFW.GLFW_KEY_UNKNOWN,
                TRIGGERBOT_CATEGORY
        ));
        ClientTickEvents.END_CLIENT_TICK.register(this::onClientTick);
        debug("Auto stun tick hook registered");
    }

    private void onClientTick(MinecraftClient client) {
        tickCounter++;
        handleToggleKey(client);
        if (!enabled) {
            return;
        }

        ClientPlayerEntity player = client.player;
        if (player == null || client.world == null || client.interactionManager == null) {
            clearSequence(null);
            return;
        }

        if (client.currentScreen != null || player.isSpectator() || player.getAbilities().creativeMode) {
            clearSequence(player);
            return;
        }

        debugTick();

        if (processActiveSequence(client, player)) {
            return;
        }

        PlayerEntity target = findTarget(client, player);
        if (target == null) {
            shieldStartTimes.clear();
            return;
        }

        shieldStartTimes.keySet().removeIf(uuid -> !uuid.equals(target.getUuid()));

        boolean targetFacingPlayer = isFacingShield(player, target);
        if (!canBreakShield(client, player, target)) {
            return;
        }

        int selectedSlot = player.getInventory().getSelectedSlot();
        ItemStack heldStack = player.getMainHandStack();
        boolean holdingAxe = isAxe(heldStack);
        boolean holdingSword = isSword(heldStack);
        int axeSlot = holdingAxe ? selectedSlot : findAxeSlot(player);
        if (!holdingAxe && !holdingSword) {
            return;
        }

        if (targetFacingPlayer && axeSlot == -1) {
            debug("Target is facing shield; no axe slot available");
            return;
        }

        if (!targetFacingPlayer && !holdingAxe) {
            debug("Behind target; not swapping from sword");
            return;
        }

        startSequence(player, target, selectedSlot, axeSlot, targetFacingPlayer);
    }

    private void handleToggleKey(MinecraftClient client) {
        if (toggleKey == null) {
            return;
        }

        while (toggleKey.wasPressed()) {
            enabled = !enabled;
            if (!enabled && client.player != null) {
                clearSequence(client.player);
            } else if (!enabled) {
                clearSequence(null);
            }
            shieldStartTimes.clear();
            sendToggleMessage(client, enabled);
        }
    }

    private void sendToggleMessage(MinecraftClient client, boolean enabled) {
        if (client.player == null) {
            return;
        }

        String message = enabled ? "Auto stun enabled" : "Auto stun disabled";
        client.player.sendMessage(Text.literal(message), false);
        debug(message);
    }

    private boolean processActiveSequence(MinecraftClient client, ClientPlayerEntity player) {
        if (activeSequence == null) {
            return false;
        }

        PlayerEntity target = getSequenceTarget(client);
        if (target == null || !isValidSequenceTarget(player, target)) {
            clearSequence(player);
            return true;
        }

        if (actionDelayTicks > 0) {
            debug("Waiting " + actionDelayTicks + " tick(s) before next action");
            actionDelayTicks--;
            return true;
        }

        StunAction action = actionQueue.peek();
        if (action == null) {
            clearFinishedSequence();
            return true;
        }

        if (!executeAction(client, player, target, action)) {
            return true;
        }

        debug("Action performed: " + action);
        actionQueue.poll();
        if (actionQueue.isEmpty()) {
            clearFinishedSequence();
        } else {
            scheduleNextActionDelay();
        }

        return true;
    }

    private PlayerEntity getSequenceTarget(MinecraftClient client) {
        if (activeSequence == null || client.world == null) {
            return null;
        }

        if (client.world.getEntityById(activeSequence.targetId()) instanceof PlayerEntity target) {
            return target;
        }

        return null;
    }

    private boolean executeAction(MinecraftClient client, ClientPlayerEntity player, PlayerEntity target, StunAction action) {
        return switch (action) {
            case SWORD_ATTACK -> attackIfReady(client, player, target);
            case SWAP_TO_AXE -> {
                swapToSlot(player, activeSequence.axeSlot());
                yield true;
            }
            case AXE_ATTACK -> {
                if (!isAxe(player.getMainHandStack())) {
                    swapToSlot(player, activeSequence.axeSlot());
                    yield false;
                }

                yield attackIfReady(client, player, target);
            }
            case SWAP_TO_ORIGINAL -> {
                swapToSlot(player, activeSequence.originalSlot());
                yield true;
            }
        };
    }

    private boolean attackIfReady(MinecraftClient client, ClientPlayerEntity player, PlayerEntity target) {
        if (lastAttackTick == tickCounter) {
            return false;
        }

        if (player.getAttackCooldownProgress(0.5f) < MIN_ATTACK_COOLDOWN) {
            return false;
        }

        client.interactionManager.attackEntity(player, target);
        player.swingHand(Hand.MAIN_HAND);
        lastAttackTick = tickCounter;
        return true;
    }

    private void startSequence(
            ClientPlayerEntity player,
            PlayerEntity target,
            int originalSlot,
            int axeSlot,
            boolean targetFacingPlayer
    ) {
        activeSequence = new StunSequence(target.getId(), originalSlot, axeSlot);
        actionQueue.clear();
        actionDelayTicks = MIN_ACTION_DELAY_TICKS;

        if (isAxe(player.getMainHandStack())) {
            actionQueue.add(StunAction.AXE_ATTACK);
            actionQueue.add(StunAction.AXE_ATTACK);
            debug("Started axe double-click stun sequence");
            return;
        }

        if (!isSword(player.getMainHandStack())) {
            clearSequence(player);
            return;
        }

        if (targetFacingPlayer) {
            actionQueue.add(StunAction.SWORD_ATTACK);
            actionQueue.add(StunAction.SWAP_TO_AXE);
            actionQueue.add(StunAction.AXE_ATTACK);
            actionQueue.add(StunAction.AXE_ATTACK);
            actionQueue.add(StunAction.SWAP_TO_ORIGINAL);
            debug("Started sword-to-axe shield-facing stun sequence");
            return;
        }

        actionQueue.add(StunAction.SWORD_ATTACK);
        debug("Started no-swap backstab stun sequence");
    }

    private void scheduleNextActionDelay() {
        actionDelayTicks = randomActionDelayTicks();
    }

    private int randomActionDelayTicks() {
        return MIN_ACTION_DELAY_TICKS + random.nextInt(RANDOM_EXTRA_DELAY_TICKS + 1);
    }

    private void clearSequence(ClientPlayerEntity player) {
        if (player != null && activeSequence != null) {
            swapToSlot(player, activeSequence.originalSlot());
        }

        clearFinishedSequence();
    }

    private void clearFinishedSequence() {
        activeSequence = null;
        actionQueue.clear();
        actionDelayTicks = 0;
    }

    private PlayerEntity findTarget(MinecraftClient client, ClientPlayerEntity player) {
        if (client.world == null) {
            return null;
        }

        PlayerEntity crosshairTarget = getCrosshairTarget(client);
        if (crosshairTarget != null && isValidTarget(player, crosshairTarget)) {
            return crosshairTarget;
        }

        PlayerEntity bestTarget = null;
        double bestDistance = MAX_ATTACK_RANGE * MAX_ATTACK_RANGE;
        for (PlayerEntity target : client.world.getPlayers()) {
            if (!isValidTarget(player, target)) {
                continue;
            }

            double distance = player.squaredDistanceTo(target);
            if (distance < bestDistance && isInFrontOfPlayer(player, target)) {
                bestDistance = distance;
                bestTarget = target;
            }
        }

        return bestTarget;
    }

    private PlayerEntity getCrosshairTarget(MinecraftClient client) {
        if (client.targetedEntity instanceof PlayerEntity target) {
            return target;
        }

        return null;
    }

    private boolean isValidTarget(ClientPlayerEntity player, PlayerEntity target) {
        return target != player
                && target.isAlive()
                && !target.isSpectator()
                && player.squaredDistanceTo(target) <= MAX_ATTACK_RANGE * MAX_ATTACK_RANGE
                && target.isBlocking()
                && isHoldingShield(target);
    }

    private boolean isValidSequenceTarget(ClientPlayerEntity player, PlayerEntity target) {
        return target != player
                && target.isAlive()
                && !target.isSpectator()
                && player.squaredDistanceTo(target) <= MAX_ATTACK_RANGE * MAX_ATTACK_RANGE;
    }

    private boolean canBreakShield(MinecraftClient client, ClientPlayerEntity player, PlayerEntity target) {
        if (!player.isSprinting() || !player.isOnGround()) {
            return false;
        }

        if (player.isUsingItem() || player.getVelocity().getY() > 0.0D) {
            return false;
        }

        return hasShieldedLongEnough(client, target);
    }

    private boolean hasShieldedLongEnough(MinecraftClient client, LivingEntity target) {
        long now = System.currentTimeMillis();
        long shieldStartTime = shieldStartTimes.computeIfAbsent(target.getUuid(), ignored -> now);
        return now - shieldStartTime >= BASE_SHIELD_DELAY_MS + getTargetPing(client, target);
    }

    private int getTargetPing(MinecraftClient client, LivingEntity target) {
        if (!(target instanceof PlayerEntity player) || client.getNetworkHandler() == null) {
            return 0;
        }

        PlayerListEntry entry = client.getNetworkHandler().getPlayerListEntry(player.getUuid());
        if (entry == null) {
            return 0;
        }

        return Math.max(entry.getLatency(), 0);
    }

    private boolean isFacingShield(ClientPlayerEntity player, LivingEntity target) {
        Vec3d targetToPlayer = player.getEntityPos().subtract(target.getEntityPos()).normalize();
        Vec3d targetLook = target.getRotationVec(1.0f).normalize();
        return targetLook.dotProduct(targetToPlayer) >= 0.0D;
    }

    private boolean isInFrontOfPlayer(ClientPlayerEntity player, LivingEntity target) {
        Vec3d playerLook = player.getRotationVec(1.0f).normalize();
        Vec3d playerToTarget = target.getEntityPos().subtract(player.getEntityPos()).normalize();
        return playerLook.dotProduct(playerToTarget) > 0.92D;
    }

    private int findAxeSlot(ClientPlayerEntity player) {
        for (int slot = HOTBAR_START_SLOT; slot < HOTBAR_SLOT_COUNT; slot++) {
            ItemStack stack = player.getInventory().getStack(slot);
            if (isAxe(stack)) {
                return slot;
            }
        }

        return -1;
    }

    private boolean isAxe(ItemStack stack) {
        return stack.isIn(ItemTags.AXES);
    }

    private boolean isSword(ItemStack stack) {
        return stack.isIn(ItemTags.SWORDS);
    }

    private boolean isHoldingShield(PlayerEntity player) {
        return player.getMainHandStack().isOf(Items.SHIELD) || player.getOffHandStack().isOf(Items.SHIELD);
    }

    private void swapToSlot(ClientPlayerEntity player, int slot) {
        if (!isHotbarSlot(slot)) {
            debug("Ignored invalid hotbar slot " + slot);
            return;
        }

        player.getInventory().setSelectedSlot(slot);
    }

    private void debugTick() {
        if (tickCounter - lastDebugTick < DEBUG_TICK_INTERVAL) {
            return;
        }

        lastDebugTick = tickCounter;
        debug("Tick executed: queue=" + actionQueue.size() + ", delay=" + actionDelayTicks);
    }

    private void debug(String message) {
        if (!DEBUG_LOGGING) {
            return;
        }

        System.out.println("[TriggerBot/AutoStun] " + message);
    }

    private boolean isHotbarSlot(int slot) {
        return slot >= HOTBAR_START_SLOT && slot < HOTBAR_SLOT_COUNT;
    }

    private enum StunAction {
        SWORD_ATTACK,
        SWAP_TO_AXE,
        AXE_ATTACK,
        SWAP_TO_ORIGINAL
    }

    private record StunSequence(int targetId, int originalSlot, int axeSlot) {
    }
}
