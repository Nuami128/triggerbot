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
    @Inject(method = "tick", at = @At("HEAD"))
    private void onTickStart(CallbackInfo ci) {
        TriggerBotMod.getModuleManager().postMovementAll();
    }

    // THE POST-TICK PIPELINE: Injecting exactly at the TAIL of tick().
    // This executes after Minecraft handles inputs and clears keys, giving
    // your module the ultimate window to inject an un-erasable Spacebar press.
    @Inject(method = "tick", at = @At("TAIL"))
    private void onTickEnd(CallbackInfo ci) {
        TriggerBotMod.getModuleManager().jumpResetAll();
    }
}
