package com.example.triggerbot.module.impl;

import com.example.triggerbot.module.ClientModule;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;

import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerInventory;

import net.minecraft.item.AxeItem;
import net.minecraft.item.ItemStack;

import net.minecraft.network.packet.c2s.play.UpdateSelectedSlotC2SPacket;

import net.minecraft.util.Hand;

import org.lwjgl.glfw.GLFW;

public class AutoStunModule implements ClientModule {

    private static final String MODULE_NAME = "AutoStun";

    // ✅ Proper Fabric keybind
    public static final KeyBinding KEYBIND = new KeyBinding(
            "key.triggerbot.autostun",
            InputUtil.Type.KEYSYM,
            GLFW.GLFW_KEY_R,
            KeyBinding.Category.MISC
    );

    private boolean enabled = false;

    @Override
    public String getName() {
        return MODULE_NAME;
    }

    public boolean isEnabled() {
        return enabled;
    }

    @Override
    public void onEnable() {
        enabled = true;
        sendHotbarMessage("AutoStun Enabled");
    }

    @Override
    public void onDisable() {
        enabled = false;
        sendHotbarMessage("AutoStun Disabled");
    }

    @Override
    public void onTick() {
        // nothing yet
    }

    // ✅ Trigger method required by TriggerBotMod
    public void trigger() {

        MinecraftClient mc = MinecraftClient.getInstance();

        if (mc.player == null || mc.world == null) {
            return;
        }

        Entity target = findNearestTarget(mc, mc.player);

        if (target == null) {
            sendHotbarMessage("No target found");
            return;
        }

        int axeSlot = findHotbarSlot(mc.player.getInventory(), AxeItem.class);

        if (axeSlot == -1) {
            sendHotbarMessage("No axe found");
            return;
        }

        int oldSlot = mc.player.getInventory().getSelectedSlot();

        swapToSlot(mc, axeSlot);

        attack(mc, target);

        swapToSlot(mc, oldSlot);

        sendHotbarMessage("Triggered");
    }

    private void attack(MinecraftClient mc, Entity target) {

        if (mc.interactionManager == null) return;

        mc.interactionManager.attackEntity(mc.player, target);

        mc.player.swingHand(Hand.MAIN_HAND);
    }

    private void swapToSlot(MinecraftClient mc, int slot) {

        mc.player.getInventory().setSelectedSlot(slot);

        if (mc.getNetworkHandler() != null) {

            mc.getNetworkHandler().sendPacket(
                    new UpdateSelectedSlotC2SPacket(slot)
            );
        }
    }

    private Entity findNearestTarget(MinecraftClient mc, ClientPlayerEntity player) {

        Entity closest = null;

        double closestDist = Double.MAX_VALUE;

        for (Entity e : mc.world.getEntities()) {

            if (!(e instanceof ClientPlayerEntity)) continue;

            if (e == player) continue;

            double dist = player.squaredDistanceTo(e);

            if (dist > 16.0) continue;

            if (dist < closestDist) {

                closestDist = dist;

                closest = e;
            }
        }

        return closest;
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

    private void sendHotbarMessage(String msg) {

        MinecraftClient mc = MinecraftClient.getInstance();

        if (mc.player == null) return;

        mc.player.sendMessage(
                net.minecraft.text.Text.literal("[AutoStun] " + msg),
                true
        );
    }
}
