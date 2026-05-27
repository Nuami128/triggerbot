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

    // No refMap is loaded at runtime on Mojo, so Yarn name "damage" is NOT resolved.
    // We must use the intermediary name directly: method_6099
    // This is LivingEntity.damage(DamageSource, float) in 1.21.x intermediary mappings.
    @Inject(method = "method_6099(Lnet/minecraft/class_1282;F)Z", at = @At("HEAD"))
    private void onDamage(DamageSource source, float amount, CallbackInfoReturnable<Boolean> cir) {
        System.out.println("[TriggerBot] damage mixin fired, amount=" + amount);

        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player == null) return;
        if ((Object) this != mc.player) return;

        System.out.println("[TriggerBot] Player took damage! Calling onDamageTaken.");

        ModuleManager.getInstance().find("Auto Jump Reset").ifPresent(m -> {
            if (m instanceof AutoJumpResetModule ajr) {
                ajr.onDamageTaken();
            }
        });
    }
}
