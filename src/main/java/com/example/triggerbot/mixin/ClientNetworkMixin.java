package com.example.triggerbot.mixin;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.network.packet.s2c.play.EntityVelocityUpdateS2CPacket;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ClientPlayNetworkHandler.class)
public class ClientNetworkMixin {

    @Inject(method = "onEntityVelocityUpdate", at = @At("HEAD"))
    private void onVelocityUpdate(EntityVelocityUpdateS2CPacket packet, CallbackInfo ci) {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc == null || mc.player == null) return;
        if (packet.getEntityId() != mc.player.getId()) return;

        System.out.println("[AJR] Velocity packet received | onGround: " + mc.player.isOnGround() + " | thread: " + Thread.currentThread().getName());

        if (!mc.player.isOnGround()) return;

        mc.player.jump();
    }
}
