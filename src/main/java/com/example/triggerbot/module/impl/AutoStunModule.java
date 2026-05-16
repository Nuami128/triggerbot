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
import net.minecraft.item.AxeItem;
import net.minecraft.item.ItemStack;
import net.minecraft.item.SwordItem;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.math.Vec3d;
import org.lwjgl.glfw.GLFW;

public class AutoStunModule extends EmptyModule {
    private static final float MIN_ATTACK_COOLDOWN = 0.9f;
    private static final long BASE_SHIELD_DELAY_MS = 250L;
    private static final long MIN_CLICK_INTERVAL_MS = 500L;

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
                "category.triggerbot"
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

        int selectedSlot = player.getInventory().selectedSlot;
        ItemStack heldStack = player.getMainHandStack();
        if (heldStack.getItem() instanceof AxeItem) {
            clickWithAxe(client, player, target, now);
            return;
        }

        if (!(heldStack.getItem() instanceof SwordItem)) {
            return;
        }

        int axeSlot = findAxeSlot(player);
        if (axeSlot == -1) {
            return;
        }

        swapToSlot(client, player, axeSlot);
        clickWithAxe(client, player, target, now);
        pendingStun = new PendingStun(target.getId(), selectedSlot, axeSlot);
        swapToSlot(client, player, selectedSlot);
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
            swapToSlot(client, player, pendingStun.originalSlot);
            pendingStun = null;
            return true;
        }

        long now = System.currentTimeMillis();
        if (now - lastClickTime < MIN_CLICK_INTERVAL_MS) {
            return true;
        }

        Entity entity = client.world.getEntityById(pendingStun.targetId);
        if (!(entity instanceof LivingEntity target) || !isTargetUnderCrosshair(client, pendingStun.targetId)) {
            swapToSlot(client, player, pendingStun.originalSlot);
            pendingStun = null;
            return true;
        }

        swapToSlot(client, player, pendingStun.axeSlot);
        clickWithAxe(client, player, target, now);
        swapToSlot(client, player, pendingStun.originalSlot);
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
        Vec3d targetToPlayer = player.getPos().subtract(target.getPos()).normalize();
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
        simulateClick(client.options.attackKey);
        client.interactionManager.attackEntity(player, target);
        player.swingHand(Hand.MAIN_HAND);
        lastClickTime = now;
    }

    private int findAxeSlot(ClientPlayerEntity player) {
        for (int slot = 0; slot < 9; slot++) {
            ItemStack stack = player.getInventory().getStack(slot);
            if (stack.getItem() instanceof AxeItem) {
                return slot;
            }
        }

        return -1;
    }

    private void swapToSlot(MinecraftClient client, ClientPlayerEntity player, int slot) {
        simulateKeyPress(client.options.hotbarKeys[slot]);
        player.getInventory().selectedSlot = slot;
    }

    private void simulateKeyPress(KeyBinding keyBinding) {
        InputUtil.Key boundKey = keyBinding.getBoundKey();
        KeyBinding.setKeyPressed(boundKey, true);
        KeyBinding.onKeyPressed(boundKey);
        KeyBinding.setKeyPressed(boundKey, false);
    }

    private void simulateClick(KeyBinding keyBinding) {
        InputUtil.Key boundKey = keyBinding.getBoundKey();
        KeyBinding.setKeyPressed(boundKey, true);
        KeyBinding.setKeyPressed(boundKey, false);
    }

    private record PendingStun(int targetId, int originalSlot, int axeSlot) {
    }
}
