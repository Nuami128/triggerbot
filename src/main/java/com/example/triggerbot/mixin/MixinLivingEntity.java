package com.example.triggerbot.mixin;

import com.example.triggerbot.module.ModuleManager;
import com.example.triggerbot.module.impl.AutoJumpResetModule;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.damage.DamageSource;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(LivingEntity.class)
public class MixinLivingEntity {

    // In Yarn mappings for 1.21.x, LivingEntity.damage() takes (DamageSource, float)
    // There is NO ServerWorld parameter — that was a different mapping/version.
    // If this still won't inject, check exact Yarn name with: ./gradlew --info build
    // and search for "damage" in the LivingEntity class in the mappings jar.
    @Inject(method = "damage(Lnet/minecraft/entity/damage/DamageSource;F)Z", at = @At("HEAD"))
    private void onDamage(DamageSource source, float amount, CallbackInfoReturnable<Boolean> cir) {
        // Diagnostic: always fires if mixin is working at all
        System.out.println("[TriggerBot] MixinLivingEntity.onDamage fired, amount=" + amount);

        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player == null) {
            System.out.println("[TriggerBot] mc.player is null, skipping");
            return;
        }

        // 'this' is the LivingEntity being damaged.
        // On the client, mc.player is a ClientPlayerEntity which IS a LivingEntity.
        // We compare by reference to check if it's our player.
        if ((Object) this != mc.player) {
            System.out.println("[TriggerBot] Not our player (entity=" + ((LivingEntity)(Object)this).getClass().getSimpleName() + "), skipping");
            return;
        }

        System.out.println("[TriggerBot] Damage taken by player! Calling onDamageTaken.");

        ModuleManager.getInstance().find("Auto Jump Reset").ifPresent(m -> {
            if (m instanceof AutoJumpResetModule ajr) {
                ajr.onDamageTaken();
            } else {
                System.out.println("[TriggerBot] Module found but wrong type: " + m.getClass().getName());
            }
        });

        if (ModuleManager.getInstance().find("Auto Jump Reset").isEmpty()) {
            System.out.println("[TriggerBot] 'Auto Jump Reset' module NOT found in ModuleManager!");
        }
    }
}
