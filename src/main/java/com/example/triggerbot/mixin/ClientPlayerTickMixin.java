package com.example.triggerbot.mixin;

import com.example.triggerbot.TriggerBotMod;
import net.minecraft.client.network.ClientPlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ClientPlayerEntity.class)
public class ClientPlayerTickMixin {

    // Fires BEFORE sendMovementPackets — safe for movement/sprint/jump state changes.
    // tickAll and clientTickAll go here because they set key state that needs to be
    // included in the outgoing packet, not read back after it's already sent.
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

    // Fires AFTER sendMovementPackets — the position packet is now committed to the
    // server. Grim's simulation sees: move → packet → attack, in the correct order.
    // Firing attacks before this point is what caused the Simulation flag cascade.
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
