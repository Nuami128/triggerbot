package com.example.triggerbot.module.impl;

import com.example.triggerbot.module.EmptyModule;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.ingame.InventoryScreen;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Hand;

public class InventoryEatModule extends EmptyModule {

    private boolean pendingEat = false;

    public InventoryEatModule() {
        super("Inventory Eat");
    }

    // Called from mixin when inventory opens
    public void scheduleEat() {
        pendingEat = true;
    }

    @Override
    public void onTick() {
        MinecraftClient mc = MinecraftClient.getInstance();

        if (mc.player == null || mc.world == null) return;
        if (mc.interactionManager == null) return;

        // If inventory closed, reset
        if (!(mc.currentScreen instanceof InventoryScreen)) {
            pendingEat = false;
            return;
        }

        if (mc.player.getHungerManager().getFoodLevel() >= 20) return;

        Hand hand = getEatingHand(mc);
        if (hand == null) return;

        // Fire right click on the tick after inventory opens
        if (pendingEat) {
            mc.interactionManager.interactItem(mc.player, hand);
            pendingEat = false;
            return;
        }

        // Keep eating if not finished
        if (mc.player.isUsingItem()) {
            mc.interactionManager.interactItem(mc.player, hand);
        }
    }

    private Hand getEatingHand(MinecraftClient mc) {
        if (mc.player == null) return null;

        ItemStack offhand = mc.player.getOffHandStack();
        ItemStack mainhand = mc.player.getMainHandStack();

        if (isFood(offhand)) return Hand.OFF_HAND;
        if (isFood(mainhand)) return Hand.MAIN_HAND;

        return null;
    }

    private boolean isFood(ItemStack stack) {
        if (stack == null || stack.isEmpty()) return false;
        return stack.contains(DataComponentTypes.FOOD);
    }
}
