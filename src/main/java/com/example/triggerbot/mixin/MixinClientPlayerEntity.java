package com.example.triggerbot.mixin;

import com.example.triggerbot.module.ModuleManager;
import com.example.triggerbot.module.impl.AutoJumpResetModule;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.entity.damage.DamageSource;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ClientPlayerEntity.class)
public class MixinClientPlayerEntity {

    @Inject(method = "tick", at = @At("HEAD"))
    private void onTickStart(CallbackInfo ci) {
        ModuleManager.getInstance().postMovementAll();
    }

    @Inject(method = "damage", at = @At("HEAD"))
    private void onDamage(DamageSource source, float amount, CallbackInfoReturnable<Boolean> cir) {
        ModuleManager.getInstance().find("Auto Jump Reset").ifPresent(m -> {
            if (m instanceof AutoJumpResetModule ajr) {
                ajr.onDamageTaken();
            }
        });
    }
}
