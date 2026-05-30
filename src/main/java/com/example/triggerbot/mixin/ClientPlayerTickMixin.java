package com.example.triggerbot.mixin;

import com.example.triggerbot.TriggerBotMod;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ClientPlayerEntity.class)
public class ClientPlayerTickMixin {

    private boolean wasHurt = false;

    // Preserved at HEAD to prevent your Post flags from returning
    @Inject(method = "tick", at = @At("HEAD"))
    private void onTickStart(CallbackInfo ci) {
        TriggerBotMod.getModuleManager().postMovementAll();
    }

    // Unambiguous and bulletproof: Injecting directly after movement keys are mapped
    @Inject(method = "tickMovement", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/network/ClientPlayerEntity;hasMovementInput()Z"))
    private void onTickMovementAfterInput(CallbackInfo ci) {
        TriggerBotMod.getModuleManager().jumpResetAll();
    }

    @Inject(method = "tick", at = @At("HEAD"))
    private void onTickDebug(CallbackInfo ci) {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player == null) return;

        boolean hurtNow = mc.player.hurtTime > 0;
        if (hurtNow && !wasHurt) {
            System.out.println("[TriggerBot] DAMAGE DETECTED");
        }
        wasHurt = hurtNow;
    }
}
