package com.example.triggerbot.mixin;

import com.example.triggerbot.TriggerBotMod;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.network.packet.s2c.play.EntityVelocityUpdateS2CPacket;
import net.minecraft.network.packet.s2c.play.EntityStatusS2CPacket;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ClientPlayNetworkHandler.class)
public class ClientNetworkMixin {

    @Inject(method = "onEntityVelocityUpdate", at = @At("TAIL"))
    private void onVelocityUpdate(EntityVelocityUpdateS2CPacket packet, CallbackInfo ci) {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc == null || mc.player == null) return;
        if (packet.getEntityId() != mc.player.getId()) return;

        // Schedule on main thread, after velocity is fully applied
        mc.execute(() -> {
            if (mc.player == null) return;
            if (!mc.player.isOnGround()) return;
            mc.player.jump();
        });
    }

    @Inject(method = "onEntityStatus", at = @At("HEAD"))
    private void onEntityStatusPacket(EntityStatusS2CPacket packet, CallbackInfo ci) {
        // intentionally empty
    }
}
