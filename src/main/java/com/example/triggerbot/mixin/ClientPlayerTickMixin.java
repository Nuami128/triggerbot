package com.example.triggerbot.mixin;

import com.example.triggerbot.TriggerBotMod;
import net.minecraft.client.network.ClientPlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ClientPlayerEntity.class)
public class ClientPlayerTickMixin {

    // tickAll and clientTickAll fire BEFORE sendMovementPackets so that
    // jump/sprint key state is included in the outgoing position packet.
    @Inject(
        method = "tick",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/network/ClientPlayerEntity;sendMovementPackets()V",
            shift = At.Shift.BEFORE
        ),
        remap = true
    )
    private void beforeMovementPackets(CallbackInfo ci) {
        TriggerBotMod.getModuleManager().tickAll();
        TriggerBotMod.getModuleManager().clientTickAll();
    }

    // postMovementAll fires at TAIL (end of tick) — after sendMovementPackets
    // AND after the superclass tick has fully completed. This means the position
    // packet is already sent, and the INTERACT_ENTITY + ANIMATION packets from
    // invokeDoAttack() arrive at the server in the correct post-movement order
    // that Grim's Post and PacketOrderO checks expect on 1.21.11.
    @Inject(
        method = "tick",
        at = @At("TAIL"),
        remap = true
    )
    private void onTickTail(CallbackInfo ci) {
        TriggerBotMod.getModuleManager().postMovementAll();
    }
}
