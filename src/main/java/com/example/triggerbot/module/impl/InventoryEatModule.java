package com.example.triggerbot.module.impl;

import com.example.triggerbot.module.EmptyModule;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.ingame.InventoryScreen;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;

public class InventoryEatModule extends EmptyModule {

    private boolean pendingEat = false;

    public InventoryEatModule() {
        super("Inventory Eat");
    }

    public void scheduleEat() {
        pendingEat = true;
    }

    @Override
    public void onTick() {
        MinecraftClient mc = MinecraftClient.getInstance();

        if (mc.player == null || mc.world == null) return;
        if (mc.interactionManager == null) return;
        if (!(mc.currentScreen instanceof InventoryScreen)) {
            pendingEat = false;
            return;
        }

        if (mc.player.getHungerManager().getFoodLevel() >= 20) return;

        Hand hand = getEatingHand(mc);
        if (hand == null) return;

        if (pendingEat || mc.player.isUsingItem()) {
            // Temporarily close screen, interact, reopen
            mc.player.networkHandler.sendPacket(
                new net.minecraft.network.packet.c2s.play.CloseHandledScreenC2SPacket(0)
            );
            mc.interactionManager.interactItem(mc.player, hand);
            pendingEat = false;
        }
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
