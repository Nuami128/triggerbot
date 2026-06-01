package com.example.triggerbot.mixin;

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

    @Inject(method = "handlePacket", at = @At("HEAD"), cancellable = true)
    private static <T extends PacketListener> void onHandlePacket(Packet<T> packet, T listener, CallbackInfo ci) {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc == null || mc.player == null) return;

        if (packet instanceof EntityVelocityUpdateS2CPacket velocityPacket) {
            if (velocityPacket.getEntityId() != mc.player.getId()) return;

            // Cancel the normal packet handling
            ci.cancel();

            // Schedule on main thread: jump first, then apply velocity
            mc.execute(() -> {
                if (mc.player == null) return;

                // Jump before velocity is applied
                if (mc.player.isOnGround()) {
                    mc.player.jump();
                }

                // Manually apply the velocity packet after jumping
                mc.player.setVelocity(
                    velocityPacket.getVelocityX() / 8000.0,
                    velocityPacket.getVelocityY() / 8000.0,
                    velocityPacket.getVelocityZ() / 8000.0
                );
            });
        }
    }
}
