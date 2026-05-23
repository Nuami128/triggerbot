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

        // Keep holding the right click while inventory is open and eating
        if (mc.player.isUsingItem()) {
            mc.interactionManager.interactItem(mc.player, getEatingHand(mc));
        }
    }

    // Called from mixin when inventory screen opens
    public void tryEat() {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player == null || mc.interactionManager == null) return;
        if (mc.player.getHungerManager().getFoodLevel() >= 20) return;

        Hand hand = getEatingHand(mc);
        if (hand == null) return;

        mc.interactionManager.interactItem(mc.player, hand);
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
