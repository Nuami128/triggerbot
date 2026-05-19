package com.example.triggerbot.module.impl;

import com.example.triggerbot.module.ClientModule;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;

import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerInventory;

import net.minecraft.item.AxeItem;
import net.minecraft.item.ItemStack;

import net.minecraft.network.packet.c2s.play.UpdateSelectedSlotC2SPacket;

import net.minecraft.util.Hand;

import org.lwjgl.glfw.GLFW;

public class AutoStunModule implements ClientModule {

    public static final KeyBinding KEYBIND = new KeyBinding(
            "key.triggerbot.autostun",
            InputUtil.Type.KEYSYM,
            GLFW.GLFW_KEY_R,
            KeyBinding.Category.MISC
    );

    private boolean enabled = false;

    // tick state machine
    private enum Stage {
        IDLE,
        SWAP,
        ATTACK,
        RETURN
    }

    private Stage stage = Stage.IDLE;
    private int tickDelay = 0;

    private int savedSlot = -1;

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
        stage = Stage.IDLE;
        tickDelay = 0;
        send("Disabled");
    }

    @Override
    public void onTick() {

        if (!enabled) return;

        MinecraftClient mc = MinecraftClient.getInstance();

        if (mc.player == null || mc.world == null) return;

        // don’t interrupt eating / shielding
        if (mc.player.isUsingItem()) return;

        // cooldown gate (prevents spam CPS)
        if (mc.player.getAttackCooldownProgress(0.5f) < 0.92f) return;

        // STATE MACHINE
        if (stage == Stage.IDLE) {

            Entity target = findNearestTarget(mc, mc.player);

            if (target == null) return;

            stage = Stage.SWAP;
            return;
        }

        if (stage == Stage.SWAP) {

            savedSlot = mc.player.getInventory().getSelectedSlot();

            int axeSlot = findHotbarSlot(mc.player.getInventory(), AxeItem.class);

            if (axeSlot == -1) {
                send("No axe");
                stage = Stage.IDLE;
                return;
            }

            swap(mc, axeSlot);

            stage = Stage.ATTACK;
            tickDelay = 0;
            return;
        }

        if (stage == Stage.ATTACK) {

            if (tickDelay++ < 1) return;

            Entity target = findNearestTarget(mc, mc.player);

            if (target != null) {
                attack(mc, target);
            }

            stage = Stage.RETURN;
            tickDelay = 0;
            return;
        }

        if (stage == Stage.RETURN) {

            if (tickDelay++ < 1) return;

            if (savedSlot != -1) {
                swap(mc, savedSlot);
            }

            stage = Stage.IDLE;
        }
    }

    private void attack(MinecraftClient mc, Entity target) {

        if (mc.interactionManager == null) return;

        mc.interactionManager.attackEntity(mc.player, target);

        mc.player.swingHand(Hand.MAIN_HAND);

        send("Triggered");
    }

    private void swap(MinecraftClient mc, int slot) {

        mc.player.getInventory().setSelectedSlot(slot);

        if (mc.getNetworkHandler() != null) {

            mc.getNetworkHandler().sendPacket(
                    new UpdateSelectedSlotC2SPacket(slot)
            );
        }
    }

    private Entity findNearestTarget(
            MinecraftClient mc,
            ClientPlayerEntity player
    ) {

        Entity closest = null;

        double closestDist = Double.MAX_VALUE;

        for (Entity e : mc.world.getEntities()) {

            if (!(e instanceof LivingEntity living)) continue;

            if (e == player) continue;

            // ONLY shielders
            if (!living.isBlocking()) continue;

            double dist = player.squaredDistanceTo(e);

            // 3 blocks
            if (dist > 9.0) continue;

            if (dist < closestDist) {

                closestDist = dist;

                closest = e;
            }
        }

        return closest;
    }

    private <T> int findHotbarSlot(
            PlayerInventory inv,
            Class<T> itemClass
    ) {

        for (int i = 0; i < 9; i++) {

            ItemStack stack = inv.getStack(i);

            if (!stack.isEmpty()
                    && itemClass.isInstance(stack.getItem())) {

                return i;
            }
        }

        return -1;
    }

    private void send(String text) {

        MinecraftClient mc = MinecraftClient.getInstance();

        if (mc.player == null) return;

        mc.player.sendMessage(
                net.minecraft.text.Text.literal(
                        "[AutoStun] " + text
                ),
                true
        );
    }
}
