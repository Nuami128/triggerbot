package com.example.triggerbot.util;

import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
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
    public static boolean isFacingUs(MinecraftClient mc, Entity target) {
        if (mc.player == null) return false;

        Vec3d toPlayer = new Vec3d(
                mc.player.getX() - target.getX(),
                mc.player.getY() - target.getY(),
                mc.player.getZ() - target.getZ()
        ).normalize();

        Vec3d targetLook = target.getRotationVec(1.0f).normalize();

        return toPlayer.dotProduct(targetLook) > 0;
    }

    // 3 block reach check
    public static boolean isInReach(MinecraftClient mc, Entity target) {
        if (mc.player == null) return false;
        double dx = mc.player.getX() - target.getX();
        double dy = mc.player.getY() - target.getY();
        double dz = mc.player.getZ() - target.getZ();
        return (dx * dx + dy * dy + dz * dz) <= 9.0;
    }
}
