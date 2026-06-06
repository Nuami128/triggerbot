package com.example.triggerbot.mixin;

import com.example.triggerbot.TriggerBotMod;
import net.minecraft.client.network.ClientPlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ClientPlayerEntity.class)
public class ClientPlayerTickMixin {

    // Fires tick logic (movement keys, sprint, etc.) BEFORE the packet
    @Inject(
        method = "tick",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/network/ClientPlayerEntity;sendMovementPackets()V",
            shift = At.Shift.BEFORE
        ),
        remap = true
    )
    private void beforeMovementPackets(CallbackInfo ci) {
        TriggerBotMod.getModuleManager().tickAll();
        TriggerBotMod.getModuleManager().clientTickAll();
    }

    // Fires attack logic AFTER the packet — position is now committed to server,
    // so Grim's simulation sees: move → packet → attack, in the correct order.
    @Inject(
        method = "tick",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/network/ClientPlayerEntity;sendMovementPackets()V",
            shift = At.Shift.AFTER
        ),
        remap = true
    )
    private void afterMovementPackets(CallbackInfo ci) {
        TriggerBotMod.getModuleManager().postMovementAll();
    }
}
