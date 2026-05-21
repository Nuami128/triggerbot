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

    public static final KeyBinding KEYBIND = new KeyBinding(
            "key.triggerbot.autostun",
            InputUtil.Type.KEYSYM,
            GLFW.GLFW_KEY_R,
            KeyBinding.Category.MISC
    );

    private enum State { IDLE, SWAPPED, COOLDOWN }

    private boolean enabled = false;
    private State state = State.IDLE;
    private int originalSlot = -1;
    private int tickCounter = 0;
    private Entity cachedTarget = null;

    @Override
    public String getName() { return "AutoStun"; }

    public boolean isEnabled() { return enabled; }

    @Override
    public void onEnable() { enabled = true; send("Enabled"); }

    @Override
    public void onDisable() {
        enabled = false;
        state = State.IDLE;
        tickCounter = 0;
        cachedTarget = null;
        send("Disabled");
    }

    @Override
    public void onTick() {}

    @Override
    public void onPostMovement() {
        MinecraftClient mc = MinecraftClient.getInstance();

        if (!enabled) return;
        if (mc.player == null || mc.world == null) return;
        if (mc.currentScreen != null) return;
        if (mc.player.isDead()) return;
        if (mc.interactionManager == null) return;
        if (mc.getNetworkHandler() == null) return;

        tickCounter++;

        switch (state) {

            case IDLE -> {
                if (mc.player.getAttackCooldownProgress(0.5f) < 0.92f) return;

                cachedTarget = findTarget(mc);
                if (cachedTarget == null) return;

                int axeSlot = findAxe(mc);
                if (axeSlot == -1) return;

                originalSlot = mc.player.getInventory().getSelectedSlot();

                // Only send if slot actually changes
                if (originalSlot != axeSlot) {
                    mc.player.getInventory().setSelectedSlot(axeSlot);
                    mc.getNetworkHandler().sendPacket(
                            new UpdateSelectedSlotC2SPacket(axeSlot)
                    );
                    tickCounter = 0;
                    state = State.SWAPPED;
                }
                // If axe is already selected, skip straight to attack next tick
                else {
                    tickCounter = 0;
                    state = State.SWAPPED;
                }
            }

            case SWAPPED -> {
                // Wait 3 full ticks after slot change before attacking
                if (tickCounter < 3) return;

                if (cachedTarget == null || !cachedTarget.isAlive() || cachedTarget.isRemoved()) {
                    swapBack(mc);
                    state = State.IDLE;
                    return;
                }

                mc.interactionManager.attackEntity(mc.player, cachedTarget);
                mc.player.swingHand(Hand.MAIN_HAND);

                swapBack(mc);
                tickCounter = 0;
                state = State.COOLDOWN;
            }

            case COOLDOWN -> {
                if (tickCounter >= 5) {
                    cachedTarget = null;
                    state = State.IDLE;
                }
            }
        }
    }

    private void swapBack(MinecraftClient mc) {
        int current = mc.player.getInventory().getSelectedSlot();
        if (originalSlot != -1 && current != originalSlot) {
            mc.player.getInventory().setSelectedSlot(originalSlot);
            mc.getNetworkHandler().sendPacket(
                    new UpdateSelectedSlotC2SPacket(originalSlot)
            );
        }
    }

    private Entity findTarget(MinecraftClient mc) {
        Vec3d eyePos = mc.player.getEyePos();
        Vec3d look = mc.player.getRotationVec(1.0f);
        Vec3d reachVec = eyePos.add(look.multiply(3.0));

        for (Entity e : mc.world.getEntities()) {
            if (!(e instanceof LivingEntity)) continue;
            if (e == mc.player) continue;
            if (!e.isAlive()) continue;
            if (e.isRemoved()) continue;
            if (e.isSpectator()) continue;

            // Strict vanilla hitbox, no expand
            Box box = e.getBoundingBox();
            Optional<Vec3d> hit = box.raycast(eyePos, reachVec);
            if (hit.isPresent()) return e;
        }
        return null;
    }

    private int findAxe(MinecraftClient mc) {
        for (int i = 0; i < 9; i++) {
            if (mc.player.getInventory().getStack(i).getItem() instanceof AxeItem)
                return i;
        }
        return -1;
    }

    private void send(String msg) {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player == null) return;
        mc.player.sendMessage(
                net.minecraft.text.Text.literal("[AutoStun] " + msg), true
        );
    }
}
