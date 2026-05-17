package com.example.triggerbot.module.impl;

import com.example.triggerbot.module.EmptyModule;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.tag.ItemTags;
import net.minecraft.util.Hand;
import net.minecraft.util.Identifier;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.math.Vec3d;
import org.lwjgl.glfw.GLFW;

public class AutoStunModule extends EmptyModule {
    private static final float MIN_ATTACK_COOLDOWN = 0.9f;
    private static final long BASE_SHIELD_DELAY_MS = 250L;
    private static final long MIN_CLICK_INTERVAL_MS = 500L;
    private static final int HOTBAR_START_SLOT = 0;
    private static final int HOTBAR_SLOT_COUNT = 9;
    private static final KeyBinding.Category TRIGGERBOT_CATEGORY = KeyBinding.Category.create(
            Identifier.of("triggerbot", "triggerbot")
    );

    private final Map<UUID, Long> shieldStartTimes = new HashMap<>();

    private KeyBinding toggleKey;
    private PendingStun pendingStun;
    private boolean enabled = true;
    private long lastClickTime;

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
    }

    private void onClientTick(MinecraftClient client) {
        handleToggleKey();
        if (!enabled) {
            return;
        }

        if (handlePendingStun(client)) {
            return;
        }

        ClientPlayerEntity player = client.player;
        if (player == null || client.interactionManager == null) {
            return;
        }

        if (client.currentScreen != null || player.isSpectator() || player.getAbilities().creativeMode) {
            return;
        }

        if (!(client.crosshairTarget instanceof EntityHitResult entityHitResult)) {
            shieldStartTimes.clear();
            return;
        }

        Entity entity = entityHitResult.getEntity();
        if (!(entity instanceof LivingEntity target)) {
            shieldStartTimes.clear();
            return;
        }

        if (!target.isBlocking()) {
            shieldStartTimes.remove(target.getUuid());
            return;
        }

        shieldStartTimes.keySet().removeIf(uuid -> !uuid.equals(target.getUuid()));

        if (!isInsideShieldArc(player, target)) {
            return;
        }

        if (player.getVelocity().getY() > 0.0) {
            return;
        }

        if (!hasShieldedLongEnough(client, target)) {
            return;
        }

        if (player.getAttackCooldownProgress(0.5f) < MIN_ATTACK_COOLDOWN) {
            return;
        }

        long now = System.currentTimeMillis();
        if (now - lastClickTime < MIN_CLICK_INTERVAL_MS) {
            return;
        }

        int selectedSlot = player.getInventory().getSelectedSlot();
        ItemStack heldStack = player.getMainHandStack();
        if (isAxe(heldStack)) {
            clickWithAxe(client, player, target, now);
            return;
        }

        if (!isSword(heldStack)) {
            return;
        }

        int axeSlot = findAxeSlot(player);
        if (axeSlot == -1) {
            return;
        }

        swapToSlot(player, axeSlot);
        clickWithAxe(client, player, target, now);
        pendingStun = new PendingStun(target.getId(), selectedSlot, axeSlot);
        swapToSlot(player, selectedSlot);
    }

    private void handleToggleKey() {
        if (toggleKey == null) {
            return;
        }

        while (toggleKey.wasPressed()) {
            enabled = !enabled;
            pendingStun = null;
            shieldStartTimes.clear();
        }
    }

    private boolean handlePendingStun(MinecraftClient client) {
        if (pendingStun == null) {
            return false;
        }

        ClientPlayerEntity player = client.player;
        if (player == null || client.world == null || client.interactionManager == null) {
            pendingStun = null;
            return true;
        }

        if (client.currentScreen != null || player.isSpectator() || player.getAbilities().creativeMode) {
            swapToSlot(player, pendingStun.originalSlot);
            pendingStun = null;
            return true;
        }

        long now = System.currentTimeMillis();
        if (now - lastClickTime < MIN_CLICK_INTERVAL_MS) {
            return true;
        }

        Entity entity = client.world.getEntityById(pendingStun.targetId);
        if (!(entity instanceof LivingEntity target) || !isTargetUnderCrosshair(client, pendingStun.targetId)) {
            swapToSlot(player, pendingStun.originalSlot);
            pendingStun = null;
            return true;
        }

        swapToSlot(player, pendingStun.axeSlot);
        clickWithAxe(client, player, target, now);
        swapToSlot(player, pendingStun.originalSlot);
        pendingStun = null;
        return true;
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

    private boolean isInsideShieldArc(ClientPlayerEntity player, LivingEntity target) {
        Vec3d targetToPlayer = player.getEntityPos().subtract(target.getEntityPos()).normalize();
        Vec3d targetLook = target.getRotationVec(1.0f).normalize();
        return targetLook.dotProduct(targetToPlayer) >= 0.0;
    }

    private boolean isTargetUnderCrosshair(MinecraftClient client, int targetId) {
        if (!(client.crosshairTarget instanceof EntityHitResult entityHitResult)) {
            return false;
        }

        return entityHitResult.getEntity().getId() == targetId;
    }

    private void clickWithAxe(MinecraftClient client, ClientPlayerEntity player, LivingEntity target, long now) {
        client.interactionManager.attackEntity(player, target);
        player.swingHand(Hand.MAIN_HAND);
        lastClickTime = now;
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

    private void swapToSlot(ClientPlayerEntity player, int slot) {
        if (!isHotbarSlot(slot)) {
            return;
        }

        player.getInventory().setSelectedSlot(slot);
    }

    private boolean isHotbarSlot(int slot) {
        return slot >= HOTBAR_START_SLOT && slot < HOTBAR_SLOT_COUNT;
    }

    private record PendingStun(int targetId, int originalSlot, int axeSlot) {
    }
}
