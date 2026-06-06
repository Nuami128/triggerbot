package com.example.triggerbot.mixin;

import com.example.triggerbot.TriggerBotMod;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.server.world.ServerWorld;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(LivingEntity.class)
public class PlayerDamageMixin {

    // On 1.21.11 the damage method signature gained a ServerWorld parameter.
    // Without it the mixin descriptor doesn't match and crashes on load.
    @Inject(method = "damage", at = @At("HEAD"))
    private void onDamage(ServerWorld world, DamageSource source, float amount, CallbackInfoReturnable<Boolean> cir) {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc == null || mc.player == null) return;
        // Only fire for the local player, not every LivingEntity in the world
        if (!((Object) this).equals(mc.player)) return;
        TriggerBotMod.getModuleManager().onDamageAll();
    }
}
