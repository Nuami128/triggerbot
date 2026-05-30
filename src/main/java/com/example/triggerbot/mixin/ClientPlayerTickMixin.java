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

    @Inject(method = "tick", at = @At("HEAD"))
    private void onTickStart(CallbackInfo ci) {
        TriggerBotMod.getModuleManager().postMovementAll();
    }

    // TAIL window handles our safe jump reset input checks
    @Inject(method = "tickMovement", at = @At("TAIL"))
    private void onTickMovementTail(CallbackInfo ci) {
        TriggerBotMod.getModuleManager().jumpResetAll();
    }

    @Inject(method = "tick", at = @At("TAIL"))
    private void onTickEnd(CallbackInfo ci) {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc == null || mc.player == null) return;

        // AUTOMATED RE-VALIDATION: If the jump reset pressed the spacebar, 
        // release it immediately once the player leaves the ground.
        // This ensures the physics engine registers a legal vanilla keystroke length!
        if (mc.options.jumpKey.isPressed() && !mc.player.isOnGround()) {
            mc.options.jumpKey.setPressed(false);
        }
    }

    @Inject(method = "tick", at = @At("HEAD"))
    private void onTickDebug(CallbackInfo ci) {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player == null) return;

        boolean hurtNow = mc.player.hurtTime > 0;
        if (hurtNow && !wasHurt) {
            System.out.println("[TriggerBot] DAMAGE DETECTED");
        }
        wasHurt = hurtNow;
    }
}
