package com.example.triggerbot.mixin;

import com.example.triggerbot.module.ModuleManager;
import com.example.triggerbot.module.impl.AutoJumpResetModule;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.server.world.ServerWorld;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(LivingEntity.class)
public class MixinLivingEntity {

    @Inject(method = "damage", at = @At("HEAD"))
    private void onDamage(ServerWorld world, DamageSource source, float amount, CallbackInfoReturnable<Boolean> cir) {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player == null) return;
        if ((Object) this != mc.player) return;

        ModuleManager.getInstance().find("Auto Jump Reset").ifPresent(m -> {
            if (m instanceof AutoJumpResetModule ajr) {
                ajr.onDamageTaken();
            }
        });
    }
}
