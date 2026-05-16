package com.example.triggerbot.mixin;

import com.example.triggerbot.TriggerBotMod;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.ChatScreen;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.network.ClientPlayerInteractionManager;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.vehicle.AbstractMinecartEntity;
import net.minecraft.entity.vehicle.BoatEntity;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.hit.HitResult;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(MinecraftClient.class)
public class MinecraftClientMixin {
    @Shadow public ClientPlayerEntity player;
    @Shadow public Screen currentScreen;
    @Shadow public HitResult crosshairTarget;
    @Shadow public ClientPlayerInteractionManager interactionManager;

    @Inject(method = "tick", at = @At("TAIL"))
    private void triggerbot$handleAttackAssist(CallbackInfo ci) {
        if (!TriggerBotMod.attackAssist) return;
        if (player == null || interactionManager == null) return;
        if (player.isSpectator()) return;
        if (player.getAbilities().creativeMode) return;
        if (currentScreen instanceof ChatScreen) return;
        if (player.getHealth() <= 0f) return;

        if (!(crosshairTarget instanceof EntityHitResult entityHitResult)) return;
        Entity target = entityHitResult.getEntity();
        if (!(target instanceof LivingEntity livingTarget)) return;
        if (livingTarget.getHealth() <= 0f) return;

        Entity vehicle = player.getVehicle();
        if (vehicle instanceof BoatEntity || vehicle instanceof AbstractMinecartEntity) return;

        if (player.isUsingItem()) {
            if (!player.isAlive()) return;
            if (player.getAttackCooldownProgress(0.5f) < 0.85f) return;
            interactionManager.attackEntity(player, target);
            player.swingHand(Hand.MAIN_HAND);
            return;
        }

        if (player.getAttackCooldownProgress(0.5f) < 0.85f) return;
        if (player.getVelocity().getY() > -0.1) return;
        interactionManager.attackEntity(player, target);
        player.swingHand(Hand.MAIN_HAND);
    }
}

