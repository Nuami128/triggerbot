package com.example.triggerbot.module.impl;

import com.example.triggerbot.module.EmptyModule;
import java.util.ArrayDeque;
import java.util.Queue;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
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
    private static final double MAX_ATTACK_RANGE = 2.95D;
    private static final int HOTBAR_START_SLOT = 0;
    private static final int HOTBAR_SLOT_COUNT = 9;
    private static final int ACTION_DELAY_TICKS = 1;
    private static final int DEBUG_TICK_INTERVAL = 20;
    private static final boolean DEBUG_LOGGING = true;
    private static final KeyBinding.Category TRIGGERBOT_CATEGORY = KeyBinding.Category.create(
            Identifier.of("triggerbot", "triggerbot")
    );

    private final Queue<StunAction> actionQueue = new ArrayDeque<>();

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

        tryStartSequence(client, player);
    }

    private void tryStartSequence(MinecraftClient client, ClientPlayerEntity player) {
        PlayerEntity target = findTarget(client, player);
        if (target == null || !canStartShieldStun(player, target)) {
            return;
        }

        startSequence(player, target);
    }

    private void handleToggleKey(MinecraftClient client) {
        if (toggleKey == null) {
            return;
        }

        while (toggleKey.wasPressed()) {
            enabled = !enabled;
            if (enabled) {
                debug("Auto stun enabled");
            } else {
                clearSequence(client.player);
                debug("Auto stun disabled");
            }
            sendToggleMessage(client, enabled);
        }
    }

    private void sendToggleMessage(MinecraftClient client, boolean enabled) {
        if (client.player == null) {
            return;
        }

        String message = enabled ? "Auto stun enabled" : "Auto stun disabled";
        client.player.sendMessage(Text.literal(message), false);
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
            actionDelayTicks--;
            debug("Waiting " + actionDelayTicks + " tick(s) before next action");
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
            case SWAP_TO_AXE -> {
                swapToSlot(player, activeSequence.axeSlot());
                yield true;
            }
            case ATTACK -> attackIfReady(client, player, target);
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

        if (!isWithinReach(player, target)) {
            clearSequence(player);
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

    private void startSequence(ClientPlayerEntity player, PlayerEntity target) {
        int originalSlot = player.getInventory().getSelectedSlot();
        ItemStack heldStack = player.getMainHandStack();
        boolean holdingAxe = isAxe(heldStack);
        boolean holdingSword = isSword(heldStack);
        boolean targetFacingPlayer = isFacingShield(player, target);
        int axeSlot = holdingAxe ? originalSlot : findAxeSlot(player);

        if (!holdingAxe && !holdingSword) {
            return;
        }

        if (targetFacingPlayer && axeSlot == -1) {
            debug("Target is facing shield; no axe slot available");
            return;
        }

        if (!targetFacingPlayer && holdingSword) {
            queueSequence(target.getId(), originalSlot, originalSlot, StunAction.ATTACK);
            debug("Started no-swap sword backstab sequence");
            return;
        }

        if (!targetFacingPlayer && holdingAxe) {
            queueSequence(target.getId(), originalSlot, originalSlot, StunAction.ATTACK, StunAction.ATTACK);
            debug("Started no-swap axe backstab sequence");
            return;
        }

        if (holdingAxe) {
            queueSequence(target.getId(), originalSlot, originalSlot, StunAction.ATTACK, StunAction.ATTACK);
            debug("Started axe shield-break sequence");
            return;
        }

        queueSequence(
                target.getId(),
                originalSlot,
                axeSlot,
                StunAction.SWAP_TO_AXE,
                StunAction.ATTACK,
                StunAction.ATTACK,
                StunAction.SWAP_TO_ORIGINAL
        );
        debug("Started sword-to-axe shield-break sequence");
    }

    private void queueSequence(int targetId, int originalSlot, int axeSlot, StunAction... actions) {
        activeSequence = new StunSequence(targetId, originalSlot, axeSlot);
        actionQueue.clear();
        for (StunAction action : actions) {
            actionQueue.add(action);
        }
        scheduleNextActionDelay();
    }

    private void scheduleNextActionDelay() {
        actionDelayTicks = ACTION_DELAY_TICKS;
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
            if (!isValidTarget(player, target) || !isInFrontOfPlayer(player, target)) {
                continue;
            }

            double distance = player.squaredDistanceTo(target);
            if (distance < bestDistance) {
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
                && isWithinReach(player, target)
                && target.isBlocking()
                && isHoldingShield(target);
    }

    private boolean isValidSequenceTarget(ClientPlayerEntity player, PlayerEntity target) {
        return target != player
                && target.isAlive()
                && !target.isSpectator()
                && isWithinReach(player, target);
    }

    private boolean canStartShieldStun(ClientPlayerEntity player, PlayerEntity target) {
        return isHoldingShield(target)
                && player.isSprinting()
                && player.isOnGround()
                && !isAscending(player)
                && !player.isUsingItem();
    }

    private boolean isAscending(ClientPlayerEntity player) {
        return player.getVelocity().getY() > 0.0D;
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

    private boolean isWithinReach(ClientPlayerEntity player, PlayerEntity target) {
        return player.squaredDistanceTo(target) <= MAX_ATTACK_RANGE * MAX_ATTACK_RANGE;
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
        SWAP_TO_AXE,
        ATTACK,
        SWAP_TO_ORIGINAL
    }

    private record StunSequence(int targetId, int originalSlot, int axeSlot) {
    }
}
