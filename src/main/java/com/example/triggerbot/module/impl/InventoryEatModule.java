package com.example.triggerbot.module.impl;

import com.example.triggerbot.module.EmptyModule;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.ingame.InventoryScreen;
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

        // Only activate when inventory is open
        if (!(mc.currentScreen instanceof InventoryScreen)) return;

        // Check if player needs food
        if (mc.player.getHungerManager().getFoodLevel() >= 20) return;

        // Try offhand first, then main hand
        Hand hand = null;

        ItemStack offhand = mc.player.getOffHandStack();
        ItemStack mainhand = mc.player.getMainHandStack();

        if (isFood(offhand)) {
            hand = Hand.OFF_HAND;
        } else if (isFood(mainhand)) {
            hand = Hand.MAIN_HAND;
        }

        if (hand == null) return;

        // Simulate holding right click to eat
        mc.interactionManager.interactItem(mc.player, hand);
    }

    // After
private boolean isFood(ItemStack stack) {
    if (stack == null || stack.isEmpty()) return false;
    return stack.isIn(net.minecraft.registry.tag.ItemTags.FOOD);
 }
}
