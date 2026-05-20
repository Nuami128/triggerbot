package com.example.triggerbot.module.impl;

import com.example.triggerbot.module.ClientModule;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;

import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;

import net.minecraft.item.AxeItem;

import net.minecraft.network.packet.c2s.play.UpdateSelectedSlotC2SPacket;

import net.minecraft.util.Hand;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;

import org.lwjgl.glfw.GLFW;

import java.util.Optional;

public class AutoStunModule implements ClientModule {

    public static final KeyBinding KEYBIND =
            new KeyBinding(
                    "key.triggerbot.autostun",
                    InputUtil.Type.KEYSYM,
                    GLFW.GLFW_KEY_R,
                    KeyBinding.Category.MISC
            );

    private boolean enabled = false;

    private long lastActionTime = 0L;

    @Override
    public String getName() {
        return "AutoStun";
    }

    public boolean isEnabled() {
        return enabled;
    }

    @Override
    public void onEnable() {
        enabled = true;
        send("Enabled");
    }

    @Override
    public void onDisable() {
        enabled = false;
        send("Disabled");
    }

    @Override
    public void onTick() {

        MinecraftClient mc = MinecraftClient.getInstance();

        if (!enabled) return;
        if (mc.player == null || mc.world == null) return;

        // timing safety (prevents packet spam / BadPacketsA)
        if (System.currentTimeMillis() - lastActionTime < 100)
            return;

        // cooldown check
        if (mc.player.getAttackCooldownProgress(0.5f) < 0.92f)
            return;

        Entity target = findTarget(mc);
        if (target == null) return;

        int axeSlot = findAxe(mc);
        if (axeSlot == -1) return;

        // swap
        mc.player.getInventory().setSelectedSlot(axeSlot);

        if (mc.getNetworkHandler() != null) {
            mc.getNetworkHandler().sendPacket(
                    new UpdateSelectedSlotC2SPacket(axeSlot)
            );
        }

        lastActionTime = System.currentTimeMillis();

        // attack
        if (mc.interactionManager != null) {
            mc.interactionManager.attackEntity(mc.player, target);
            mc.player.swingHand(Hand.MAIN_HAND);
        }

        lastActionTime = System.currentTimeMillis();
    }

    private Entity findTarget(MinecraftClient mc) {

        Vec3d eyePos = mc.player.getEyePos();
        Vec3d look = mc.player.getRotationVec(1.0f);

        Vec3d reachVec = eyePos.add(look.multiply(3.0));

        for (Entity e : mc.world.getEntities()) {

            if (!(e instanceof LivingEntity)) continue;
            if (e == mc.player) continue;
            if (!e.isAlive()) continue;
            if (e.isSpectator()) continue;

            // STRICT VANILLA HITBOX (no expand)
            Box box = e.getBoundingBox();

            Optional<Vec3d> hit = box.raycast(eyePos, reachVec);

            if (hit.isPresent()) {
                return e;
            }
        }

        return null;
    }

    private int findAxe(MinecraftClient mc) {

        for (int i = 0; i < 9; i++) {
            if (mc.player.getInventory().getStack(i).getItem() instanceof AxeItem) {
                return i;
            }
        }

        return -1;
    }

    private void send(String msg) {
        if (MinecraftClient.getInstance().player == null) return;

        MinecraftClient.getInstance().player.sendMessage(
                net.minecraft.text.Text.literal("[AutoStun] " + msg),
                true
        );
    }
}
