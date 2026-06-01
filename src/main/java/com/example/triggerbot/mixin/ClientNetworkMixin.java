package com.example.triggerbot.mixin;

import com.example.triggerbot.TriggerBotMod;
import net.minecraft.client.MinecraftClient;
import net.minecraft.network.ClientConnection;
import net.minecraft.network.listener.PacketListener;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.s2c.play.EntityVelocityUpdateS2CPacket;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ClientConnection.class)
public class ClientNetworkMixin {

    @Inject(method = "handlePacket", at = @At("HEAD"))
    private static <T extends PacketListener> void onHandlePacket(Packet<T> packet, T listener, CallbackInfo ci) {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc == null || mc.player == null) return;

        if (packet instanceof EntityVelocityUpdateS2CPacket velocityPacket) {
            System.out.println("[AJR] Velocity packet | entityId: " + velocityPacket.getEntityId() + " | playerId: " + mc.player.getId() + " | onGround: " + mc.player.isOnGround() + " | thread: " + Thread.currentThread().getName());
            if (velocityPacket.getEntityId() == mc.player.getId()) {
                if (mc.player.isOnGround()) {
                    mc.player.jump();
                }
            }
        }
    }
}
