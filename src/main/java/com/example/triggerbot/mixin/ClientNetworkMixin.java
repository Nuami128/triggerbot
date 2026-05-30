package com.example.triggerbot.mixin;

import com.example.triggerbot.TriggerBotMod;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.network.packet.s2c.play.EntityStatusS2CPacket;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ClientPlayNetworkHandler.class)
public class ClientNetworkMixin {

    @Inject(method = "onEntityStatus", at = @At("HEAD"))
    private void onEntityStatusPacket(EntityStatusS2CPacket packet, CallbackInfo ci) {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc == null || mc.player == null || mc.world == null) return;

        // Status code 2 is the universal vanilla byte for entity damage/hurt animation
        if (packet.getEntity(mc.world) == mc.player && packet.getStatus() == 2) {
            // Instantly bypasses the 50ms tick delay loop!
            // Make sure you register an empty/stub onDamageAll() call in your ModuleManager
            TriggerBotMod.getModuleManager().onDamageAll(); 
        }
    }
}

