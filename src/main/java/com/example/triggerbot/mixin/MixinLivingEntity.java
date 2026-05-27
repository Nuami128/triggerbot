package com.example.triggerbot.mixin;

import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.damage.DamageSource;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(LivingEntity.class)
public class MixinLivingEntity {

    @Inject(method = "damage", at = @At("HEAD"))
    private void onDamage(DamageSource source, float amount, CallbackInfoReturnable<Boolean> cir) {

        MinecraftClient mc = MinecraftClient.getInstance();

        if (mc == null || mc.player == null) return;

        // only local player
        if (!((Object)this).equals(mc.player)) return;

        System.out.println("[TriggerBot] DAMAGE HOOK FIRED");
    }
}
