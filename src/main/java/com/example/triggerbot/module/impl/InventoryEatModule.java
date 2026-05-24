package com.example.triggerbot.module.impl;

import com.example.triggerbot.module.EmptyModule;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.ingame.InventoryScreen;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Hand;

public class InventoryEatModule extends EmptyModule {

    public InventoryEatModule() {
        super("Inventory Eat");
    }

    @Override
    public void onTick() {
        MinecraftClient mc = MinecraftClient.getInstance();

        if (mc.player == null || mc.world == null) return;
        if (mc.interactionManager == null) return;
        if (!(mc.currentScreen instanceof InventoryScreen)) return;
        if (mc.player.getHungerManager().getFoodLevel() >= 20) return;

        // Keep eating while inventory is open
        if (!mc.player.isUsingItem()) return;

        Hand hand = getEatingHand(mc);
        if (hand == null) return;

        mc.interactionManager.interactItem(mc.player, hand);
    }

    private Hand getEatingHand(MinecraftClient mc) {
        if (mc.player == null) return null;
        if (isFood(mc.player.getOffHandStack())) return Hand.OFF_HAND;
        if (isFood(mc.player.getMainHandStack())) return Hand.MAIN_HAND;
        return null;
    }

    private boolean isFood(ItemStack stack) {
        if (stack == null || stack.isEmpty()) return false;
        return stack.contains(DataComponentTypes.FOOD);
    }
}
