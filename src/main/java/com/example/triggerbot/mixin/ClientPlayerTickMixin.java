package com.example.triggerbot.mixin;

import com.example.triggerbot.TriggerBotMod;
import com.example.triggerbot.module.impl.TriggerBotModule;
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
        TriggerBotMod.getModuleManager().tickAll();
    }

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
        TriggerBotModule triggerBot = TriggerBotMod.getModuleManager().getModule(TriggerBotModule.class);
        if (triggerBot != null && triggerBot.isAttackPending()) {
            triggerBot.executePendingAttack();
        }
    }
}
