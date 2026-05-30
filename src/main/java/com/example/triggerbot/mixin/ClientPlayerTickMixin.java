package com.example.triggerbot.mixin;

import com.example.triggerbot.TriggerBotMod;
import net.minecraft.client.network.ClientPlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ClientPlayerEntity.class)
public class ClientPlayerTickMixin {

    // PRE-MOVEMENT: Keeps running safely at the very start of the frame loop
    @Inject(method = "tick", at = @At("TAIL"))
    private void onTickStart(CallbackInfo ci) {
        TriggerBotMod.getModuleManager().postMovementAll();
    }

    // FIXED: Changed method to tickMovement and target to HEAD.
    // This allows your module to inject the Spacebar press right before 
    // Minecraft polls keyboard inputs, making the jump fire instantly.
    @Inject(method = "tickMovement", at = @At("HEAD"))
    private void onTickMovementHead(CallbackInfo ci) {
        TriggerBotMod.getModuleManager().jumpResetAll();
    }
}
