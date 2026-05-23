
package com.example.triggerbot.util;

import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.AxeItem;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.tag.ItemTags;
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

    // Fixed reach — edge to edge ~3 blocks
    public static boolean isInReach(MinecraftClient mc, Entity target) {
        if (mc.player == null) return false;
        double dx = mc.player.getX() - target.getX();
        double dy = mc.player.getY() - target.getY();
        double dz = mc.player.getZ() - target.getZ();
        return (dx * dx + dy * dy + dz * dz) <= 14.0;
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
