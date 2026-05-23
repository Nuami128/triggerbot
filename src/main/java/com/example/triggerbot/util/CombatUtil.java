package com.example.triggerbot.util;

import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.AxeItem;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.tag.ItemTags;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;

public class CombatUtil {

    public static boolean isPlayerBusy(MinecraftClient mc) {
        if (mc.player == null) return false;
        return mc.player.isUsingItem();
    }

    public static boolean isShielding(LivingEntity entity) {
        return entity.isBlocking();
    }

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

    // Bounding box reach — measures to the actual edge of the entity box
    public static boolean isInReach(MinecraftClient mc, Entity target) {
        if (mc.player == null) return false;

        Vec3d eyePos = mc.player.getEyePos();
        Box box = target.getBoundingBox();

        // Clamp eye position to the nearest point on the bounding box
        double closestX = Math.max(box.minX, Math.min(eyePos.x, box.maxX));
        double closestY = Math.max(box.minY, Math.min(eyePos.y, box.maxY));
        double closestZ = Math.max(box.minZ, Math.min(eyePos.z, box.maxZ));

        double dx = eyePos.x - closestX;
        double dy = eyePos.y - closestY;
        double dz = eyePos.z - closestZ;

        return (dx * dx + dy * dy + dz * dz) <= 14.0; // exactly 3.0 blocks to box edge
    }

    public static boolean isSword(ItemStack stack) {
        if (stack == null || stack.isEmpty()) return false;
        return stack.isIn(ItemTags.SWORDS);
    }

    public static boolean isAxe(ItemStack stack) {
        if (stack == null || stack.isEmpty()) return false;
        return stack.getItem() instanceof AxeItem;
    }
}
