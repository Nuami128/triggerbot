package com.example.triggerbot.mixin;

import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

@Mixin(ClientPlayerEntity.class)
public class ClientPlayerEntityMixin {

    public static boolean suppressGroundSpoof = false;

    @ModifyArg(method = "sendMovementPackets", at = @At(value = "INVOKE", target = "Lnet/minecraft/network/packet/c2s/play/PlayerMoveC2SPacket$Full;<init>(DDDFFZZ)V"), index = 6)
    private boolean modifyOnGround(boolean onGround) {
        if (suppressGroundSpoof) {
            suppressGroundSpoof = false;
            return true; // Tell server we're still on ground for 1 packet
        }
        return onGround;
    }
}
