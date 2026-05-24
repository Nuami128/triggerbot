package com.example.triggerbot.mixin;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.ingame.InventoryScreen;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Hand;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(MinecraftClient.class)
public class MixinMinecraftClient {

    @Shadow public Screen currentScreen;

    @Inject(method = "setScreen", at = @At("HEAD"))
    private void onSetScreen(Screen screen, CallbackInfo ci) {
        if (!(screen instanceof InventoryScreen)) return;

        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player == null || mc.interactionManager == null) return;
        if (mc.player.getHungerManager().getFoodLevel() >= 20) return;

        // currentScreen is still null here — inventory hasn't opened yet
        // so interactItem won't be blocked
        Hand hand = null;
        ItemStack offhand = mc.player.getOffHandStack();
        ItemStack mainhand = mc.player.getMainHandStack();

        if (!offhand.isEmpty() && offhand.contains(DataComponentTypes.FOOD)) {
            hand = Hand.OFF_HAND;
        } else if (!mainhand.isEmpty() && mainhand.contains(DataComponentTypes.FOOD)) {
            hand = Hand.MAIN_HAND;
        }

        if (hand == null) return;

        // Call interactItem while screen is still null
        mc.interactionManager.interactItem(mc.player, hand);
    }
}
