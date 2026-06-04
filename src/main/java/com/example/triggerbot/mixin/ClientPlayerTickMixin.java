package com.example.triggerbot.mixin;

import com.example.triggerbot.TriggerBotMod;
import net.minecraft.client.network.ClientPlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ClientPlayerEntity.class)
public class ClientPlayerTickMixin {

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
        // 1. Process movement and auto-sprint status loops
        TriggerBotMod.getModuleManager().tickAll();
        
        // 2. Scan and execute target interactions immediately after, BEFORE positions finalize
        TriggerBotMod.getModuleManager().postMovementAll();
    }
}
