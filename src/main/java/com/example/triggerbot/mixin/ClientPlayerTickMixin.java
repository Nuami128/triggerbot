package com.example.triggerbot.mixin;

import com.example.triggerbot.TriggerBotMod;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ClientPlayerEntity.class)
public class ClientPlayerTickMixin {

    private boolean wasHurt = false;

    // 1. POST MOVEMENT: Kept at tick HEAD to preserve your custom strafe/targeting profiles
    @Inject(method = "tick", at = @At("HEAD"))
    private void onTickStart(CallbackInfo ci) {
        TriggerBotMod.getModuleManager().postMovementAll();
    }

    // 2. JUMP RESET: Moved to HEAD of tickMovement so the engine catches our simulated keypress instantly
    @Inject(method = "tickMovement", at = @At("HEAD"))
    private void onTickMovementHead(CallbackInfo ci) {
        TriggerBotMod.getModuleManager().jumpResetAll();
    }

    // 3. AUTO-RELEASE TIMER: Runs at the end of the tick loop to ensure the key isn't stuck down
    @Inject(method = "tick", at = @At("TAIL"))
    private void onTickEnd(CallbackInfo ci) {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc == null || mc.player == null) return;

        // If the module tapped space, release it on the subsequent frame to mimic a real finger lift
        if (mc.options.jumpKey.isPressed() && !mc.player.isOnGround()) {
            mc.options.jumpKey.setPressed(false);
        }
    }

    @Inject(method = "tick", at = @At("HEAD"))
    private void onTickDebug(CallbackInfo ci) {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc == null || mc.player == null) return;

        boolean hurtNow = mc.player.hurtTime > 0;
        if (hurtNow && !wasHurt) {
            System.out.println("[TriggerBot] DAMAGE DETECTED");
        }
        wasHurt = hurtNow;
    }
}
