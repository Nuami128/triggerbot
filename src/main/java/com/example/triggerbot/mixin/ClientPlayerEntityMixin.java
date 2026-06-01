package com.example.triggerbot.mixin;

import com.example.triggerbot.module.AJRState;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

@Mixin(ClientPlayerEntity.class)
public class ClientPlayerEntityMixin {

    @ModifyArg(
    method = "sendMovementPackets",
    at = @At(value = "INVOKE", target = "Lnet/minecraft/network/packet/c2s/play/PlayerMoveC2SPacket$Full;<init>(DDDFFZZ)V"),
    index = 6,
    remap = false
)
private boolean modifyOnGround(boolean onGround) {
    if (AJRState.suppressGroundSpoof) {
        AJRState.suppressGroundSpoof = false;
        return true;
    }
    return onGround;
    }
}
