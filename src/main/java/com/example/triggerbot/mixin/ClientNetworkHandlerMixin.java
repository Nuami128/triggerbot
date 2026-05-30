package com.example.triggerbot.mixin;

import com.example.triggerbot.TriggerBotMod;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.network.packet.s2c.play.EntityDamageS2CPacket;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ClientPlayNetworkHandler.class)
public class ClientNetworkHandlerMixin {

    @Inject(method = "onEntityDamage", at = @At("TAIL"))
    private void onEntityDamage(EntityDamageS2CPacket packet, CallbackInfo ci) {
        net.minecraft.client.MinecraftClient mc = net.minecraft.client.MinecraftClient.getInstance();
        if (mc.player == null) return;

        // Only trigger for our own player
        if (packet.entityId() != mc.player.getId()) return;

        // Schedule jump on main thread next tick
        mc.execute(() -> {
            if (mc.player == null) return;
            if (mc.player.isOnGround()) {
                mc.player.jump();
                System.out.println("[AutoJR] DAMAGE PACKET JUMP FIRED");
            }
        });
    }
}
