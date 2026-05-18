package com.example.triggerbot.module.impl;

import com.example.triggerbot.module.ClientModule;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.AxeItem;
import net.minecraft.item.ItemStack;
import net.minecraft.network.packet.c2s.play.UpdateSelectedSlotC2SPacket;
import net.minecraft.util.Hand;
import org.lwjgl.glfw.GLFW;

public class AutoStunModule implements ClientModule {

    private static final String MODULE_NAME = "AutoStun";

    public static final KeyBinding KEYBIND = new KeyBinding(
            "key.triggerbot.autostun",
            InputUtil.Type.KEYSYM,
            GLFW.GLFW_KEY_R,
            KeyBinding.Category.MISC
    );

    private boolean enabled = false;
    private boolean running = false;

    private enum Step {
        IDLE,
        SWITCH_TO_AXE,
        HIT_1,
        HIT_2,
        SWITCH_BACK
    }

    private Step step = Step.IDLE;

    private static final class CombatContext {
        Entity target;
        int originalSlot;
    }

    private CombatContext ctx;

    @Override
    public String getName() {
        return MODULE_NAME;
    }

    // ✅ FIX: missing method required by your TriggerBotMod
    public boolean isEnabled() {
        return enabled;
    }

    @Override
    public void onEnable() {
        enabled = true;
        sendHotbarMessage("Auto Stun Enabled");
    }

    @Override
    public void onDisable() {
        enabled = false;
        running = false;
        step = Step.IDLE;
        ctx = null;
        sendHotbarMessage("Auto Stun Disabled");
    }

    @Override
    public void onTick() {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player == null || mc.world == null) return;

        // START
        if (!running) {
            if (!enabled) return;

            if (mc.options.attackKey.wasPressed()) {
                TargetInfo target = findNearestTarget(mc, mc.player);
                if (target == null) return;

                startSequence(mc, target.entity);
            }
            return;
        }

        // RUN
        runStep(mc);
    }

    private void startSequence(MinecraftClient mc, Entity target) {
        ctx = new CombatContext();
        ctx.target = target;

        // ✅ FIX: no direct selectedSlot access
        ctx.originalSlot = mc.player.getInventory().getSelectedSlot();

        step = Step.SWITCH_TO_AXE;
        running = true;
    }

    private void runStep(MinecraftClient mc) {
        if (ctx == null || ctx.target == null) {
            reset();
            return;
        }

        switch (step) {

            case SWITCH_TO_AXE -> {
                int axeSlot = findHotbarSlot(mc.player.getInventory(), AxeItem.class);
                if (axeSlot == -1) {
                    reset();
                    return;
                }

                swap(mc, axeSlot);
                step = Step.HIT_1;
            }

            case HIT_1 -> {
                attack(mc, ctx.target);
                step = Step.HIT_2;
            }

            case HIT_2 -> {
                attack(mc, ctx.target);
                step = Step.SWITCH_BACK;
            }

            case SWITCH_BACK -> {
                swap(mc, ctx.originalSlot);
                reset();
            }
        }
    }

    private void attack(MinecraftClient mc, Entity target) {
        if (mc.player == null || mc.interactionManager == null) return;
        if (target == null) return;

        float cooldown = mc.player.getAttackCooldownProgress(0.5f);
        if (cooldown < 0.9f) return;

        mc.interactionManager.attackEntity(mc.player, target);
        mc.player.swingHand(Hand.MAIN_HAND);
    }

    private void swap(MinecraftClient mc, int slot) {
        // ✅ FIX: correct 1.21 method usage
        mc.player.getInventory().setSelectedSlot(slot);

        if (mc.getNetworkHandler() != null) {
            mc.getNetworkHandler().sendPacket(
                    new UpdateSelectedSlotC2SPacket(slot)
            );
        }
    }

    private void reset() {
        running = false;
        step = Step.IDLE;
        ctx = null;
    }

    private <T> int findHotbarSlot(PlayerInventory inv, Class<T> itemClass) {
        for (int i = 0; i < 9; i++) {
            ItemStack stack = inv.getStack(i);
            if (!stack.isEmpty() && itemClass.isInstance(stack.getItem())) {
                return i;
            }
        }
        return -1;
    }

    private TargetInfo findNearestTarget(MinecraftClient mc, ClientPlayerEntity player) {
        if (mc.world == null) return null;

        Entity closest = null;
        double closestDist = Double.MAX_VALUE;

        for (Entity e : mc.world.getEntities()) {
            if (!(e instanceof ClientPlayerEntity)) continue;
            if (e == player) continue;

            double dist = player.squaredDistanceTo(e);
            if (dist > 16.0) continue;

            if (dist < closestDist) {
                closestDist = dist;
                closest = e;
            }
        }

        if (closest == null) return null;

        return new TargetInfo(closest);
    }

    private void sendHotbarMessage(String msg) {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player == null) return;

        mc.player.sendMessage(
                net.minecraft.text.Text.literal("[AutoStun] " + msg),
                true
        );
    }

    private static class TargetInfo {
        Entity entity;

        TargetInfo(Entity entity) {
            this.entity = entity;
        }
    }
}
