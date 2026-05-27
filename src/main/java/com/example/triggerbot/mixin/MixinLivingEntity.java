package com.example.triggerbot.mixin;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.entity.damage.DamageSource;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ClientPlayerEntity.class)
public class MixinLivingEntity {

    @Inject(method = "applyDamage", at = @At("HEAD"))
    private void onDamage(DamageSource source, float amount, CallbackInfo ci) {

        MinecraftClient mc = MinecraftClient.getInstance();

        if (mc == null || mc.player == null) return;

        System.out.println("[TriggerBot] DAMAGE HOOK FIRED");
    }
}
