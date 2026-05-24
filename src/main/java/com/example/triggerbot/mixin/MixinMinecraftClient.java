package com.example.triggerbot.mixin;

import com.example.triggerbot.module.ModuleManager;
import com.example.triggerbot.module.impl.InventoryEatModule;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.ingame.InventoryScreen;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Hand;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(MinecraftClient.class)
public class MixinMinecraftClient {

    @Inject(method = "setScreen", at = @At("TAIL"))
    private void onSetScreen(Screen screen, CallbackInfo ci) {
        if (!(screen instanceof InventoryScreen)) return;

        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player == null || mc.interactionManager == null) return;

        // Check module is enabled
        var module = ModuleManager.getInstance().find("Inventory Eat");
        if (module.isEmpty()) return;
        if (!module.get().isEnabled()) return;

        if (mc.player.getHungerManager().getFoodLevel() >= 20) return;

        // Find food hand
        Hand hand = null;
        ItemStack offhand = mc.player.getOffHandStack();
        ItemStack mainhand = mc.player.getMainHandStack();

        if (!offhand.isEmpty() && offhand.contains(DataComponentTypes.FOOD)) {
            hand = Hand.OFF_HAND;
        } else if (!mainhand.isEmpty() && mainhand.contains(DataComponentTypes.FOOD)) {
            hand = Hand.MAIN_HAND;
        }

        if (hand == null) return;

        // Send interact packet in same tick as inventory open
        mc.interactionManager.interactItem(mc.player, hand);
    }
}
