package com.example.triggerbot.module.impl;

import com.example.triggerbot.module.ClientModule;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;

import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;

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

        Entity target = findTarget(mc);
        if (target == null) return;

        if (mc.interactionManager == null) return;

        mc.interactionManager.attackEntity(mc.player, target);
        mc.player.swingHand(Hand.MAIN_HAND);
    }

    private Entity findTarget(MinecraftClient mc) {

        Vec3d eyePos = mc.player.getEyePos();
        Vec3d look = mc.player.getRotationVec(1.0f);

        Vec3d reachVec = eyePos.add(
                look.x * 3.0,
                look.y * 3.0,
                look.z * 3.0
        );

        for (Entity e : mc.world.getEntities()) {

            if (!(e instanceof LivingEntity)) continue;
            if (e == mc.player) continue;
            if (!e.isAlive()) continue;

            Box box = e.getBoundingBox();

            Optional<Vec3d> hit = box.raycast(eyePos, reachVec);

            if (hit.isPresent()) {
                return e;
            }
        }

        return null;
    }

    private void send(String msg) {
        MinecraftClient mc = MinecraftClient.getInstance();

        if (mc.player == null) return;

        mc.player.sendMessage(
                net.minecraft.text.Text.literal("[AutoStun] " + msg),
                true
        );
    }
}
