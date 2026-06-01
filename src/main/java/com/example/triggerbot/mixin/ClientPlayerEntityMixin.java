package com.example.triggerbot.mixin;

import com.example.triggerbot.module.AJRState;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ClientPlayerEntity.class)
public class ClientPlayerEntityMixin {

    @Inject(method = "sendMovementPackets", at = @At("HEAD"))
    private void onSendMovementPackets(CallbackInfo ci) {
        if (!AJRState.suppressGroundSpoof) return;
        AJRState.suppressGroundSpoof = false;

        ClientPlayerEntity player = (ClientPlayerEntity)(Object) this;
        // Force ground state to true for this packet cycle
        player.setOnGround(true);
    }
}
