package com.example.triggerbot.module.impl;

import com.example.triggerbot.module.ClientModule;
import com.example.triggerbot.module.ModuleManager;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.world.item.AxeItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.SwordItem;
import net.minecraft.network.packet.c2s.play.UpdateSelectedSlotC2SPacket;
import net.minecraft.util.Hand;

import java.util.ArrayDeque;
import java.util.Deque;

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
        if (!actionQueue.isEmpty()) {
            actionQueue.poll().run();
            return;
        }

        if (!enabled) return;

        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player == null || mc.world == null) return;

        ClientPlayerEntity player = mc.player;

        if (!player.isOnGround() || player.getVelocity().y > 0 || !player.isSprinting()) return;

        // Targeting code skipped for simplicity
        // You can add your findNearestShieldTarget logic here

        // Example: always queue double-axe sequence
        queueDoubleAxeSequence(mc, player);
    }

    private void queueDoubleAxeSequence(MinecraftClient mc, ClientPlayerEntity player) {
        int axeSlot = findHotbarSlot(player.getInventory(), AxeItem.class);
        if (axeSlot == -1) return;

        previousSlot = player.getInventory().getSelectedSlot();

        actionQueue.add(() -> swapToSlot(mc, axeSlot));
        actionQueue.add(() -> performCriticalAttack(mc));
        actionQueue.add(() -> performCriticalAttack(mc));
        actionQueue.add(() -> swapToSlot(mc, previousSlot));
    }

    private void swapToSlot(MinecraftClient mc, int slot) {
        if (mc.player == null) return;
        mc.player.getInventory().setSelectedSlot(slot);
        mc.getNetworkHandler().sendPacket(new UpdateSelectedSlotC2SPacket(slot));
    }

    private void performCriticalAttack(MinecraftClient mc) {
        if (mc.player == null || mc.interactionManager == null || mc.targetedEntity == null) return;
        float cooldown = mc.player.getAttackCooldownProgress(0.5f);
        if (cooldown < 0.9f) return;

        mc.interactionManager.attackEntity(mc.player, mc.targetedEntity);
        mc.player.swingHand(Hand.MAIN_HAND);
    }

    private <T> int findHotbarSlot(PlayerInventory inv, Class<T> itemClass) {
        for (int i = 0; i < 9; i++) {
            ItemStack stack = inv.getStack(i);
            if (!stack.isEmpty() && itemClass.isInstance(stack.getItem())) {
                return i;
            }
        }
        return -1;
    }

    private void sendHotbarMessage(String message) {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player == null) return;
        mc.player.sendMessage(net.minecraft.text.Text.literal("[AutoStun] " + message), true);
    }
}
