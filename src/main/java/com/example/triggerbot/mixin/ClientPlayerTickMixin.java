package com.example.triggerbot.mixin;

import com.example.triggerbot.TriggerBotMod;
import net.minecraft.client.network.ClientPlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ClientPlayerEntity.class)
public class ClientPlayerTickMixin {

    @Inject(method = "tick", at = @At("TAIL"))
    private void onTickEnd(CallbackInfo ci) {
        TriggerBotMod.getModuleManager().tickAll();
    }

    @Inject(method = "sendMovementPackets", at = @At("TAIL"))
    private void onPostMovementPackets(CallbackInfo ci) {
        TriggerBotMod.getModuleManager().postMovementAll();
    }
}
