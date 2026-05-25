package com.example.triggerbot.mixin;

import com.example.triggerbot.module.ModuleManager;
import com.example.triggerbot.module.impl.AutoJumpResetModule;
import net.minecraft.client.network.ClientPlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ClientPlayerEntity.class)
public class MixinClientPlayerEntity {

    @Inject(method = "tick", at = @At("HEAD"))
    private void onTickStart(CallbackInfo ci) {
        ModuleManager.getInstance().postMovementAll();
    }

    @Inject(method = "updateHealth", at = @At("HEAD"))
    private void onHealthUpdate(float health, CallbackInfo ci) {
        ClientPlayerEntity player = (ClientPlayerEntity) (Object) this;
        if (health < player.getHealth()) {
            ModuleManager.getInstance().find("Auto Jump Reset").ifPresent(m -> {
                if (m instanceof AutoJumpResetModule ajr) {
                    ajr.onDamageTaken();
                }
            });
        }
    }
}
