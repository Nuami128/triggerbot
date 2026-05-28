package com.example.triggerbot.mixin;

import com.example.triggerbot.module.impl.TriggerBotModule;
import com.example.triggerbot.ModuleManager; // adjust to wherever you access your modules
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ClientPlayerEntity.class)
public class ClientPlayerTickMixin {

    private boolean wasHurt = false;

    @Inject(method = "tick", at = @At("HEAD"))
    private void onTick(CallbackInfo ci) {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player == null) return;

        boolean hurtNow = mc.player.hurtTime > 0;
        if (hurtNow && !wasHurt) {
            System.out.println("[TriggerBot] DAMAGE DETECTED");
        }
        wasHurt = hurtNow;
    }

    // This is the key fix — fires AFTER movement packets are sent each tick
    @Inject(method = "sendMovementPackets", at = @At("TAIL"))
    private void onPostMovement(CallbackInfo ci) {
        TriggerBotModule triggerBot = ModuleManager.getInstance().getTriggerBot();
        if (triggerBot != null) {
            triggerBot.onPostMovement();
        }
    }
}
