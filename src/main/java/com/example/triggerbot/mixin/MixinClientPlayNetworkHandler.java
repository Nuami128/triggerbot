package com.example.triggerbot.mixin;

import com.example.triggerbot.module.ModuleManager;
import com.example.triggerbot.module.impl.AutoJumpResetModule;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.network.packet.s2c.play.HealthUpdateS2CPacket;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ClientPlayNetworkHandler.class)
public class MixinClientPlayNetworkHandler {

    @Inject(method = "onHealthUpdate", at = @At("HEAD"))
    private void onHealthUpdate(HealthUpdateS2CPacket packet, CallbackInfo ci) {
        System.out.println("Health packet: " + packet.getHealth());
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player == null) return;

        if (packet.getHealth() < mc.player.getHealth()) {
            System.out.println("Damage detected");
            ModuleManager.getInstance().find("Auto Jump Reset").ifPresent(m -> {
                if (m instanceof AutoJumpResetModule ajr) {
                    ajr.onDamageTaken();
                }
            });
        }
    }
}
