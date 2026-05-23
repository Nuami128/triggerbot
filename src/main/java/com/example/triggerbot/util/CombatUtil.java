package com.example.triggerbot.util;

import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.Vec3d;

public class CombatUtil {

    // Check if the local player is busy (eating or shielding)
    public static boolean isPlayerBusy(MinecraftClient mc) {
        if (mc.player == null) return false;
        return mc.player.isUsingItem();
    }

    // Check if a target is actively shielding (blocking)
    public static boolean isShielding(LivingEntity entity) {
        return entity.isBlocking();
    }

    // Check if the target is facing us (shield is facing toward us)
    // Returns true if we are within 90 degrees of the target's forward facing
    public static boolean isFacingUs(MinecraftClient mc, Entity target) {
        if (mc.player == null) return false;

        Vec3d toPlayer = mc.player.getPos().subtract(target.getPos()).normalize();
        Vec3d targetLook = target.getRotationVec(1.0f).normalize();

        // Dot product > 0 means target is facing toward us
        return toPlayer.dotProduct(targetLook) > 0;
    }

    // 3 block reach check
    public static boolean isInReach(MinecraftClient mc, Entity target) {
        if (mc.player == null) return false;
        return mc.player.squaredDistanceTo(target) <= 9.0; // 3*3
    }
}
