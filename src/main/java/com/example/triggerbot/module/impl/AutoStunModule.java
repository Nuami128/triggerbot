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

import org.lwjgl.glfw.GLFW;

public class AutoStunModule implements ClientModule {

    public static final KeyBinding KEYBIND = new KeyBinding(
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

        if (!enabled)
            return;

        MinecraftClient mc = MinecraftClient.getInstance();

        if (mc.player == null || mc.world == null)
            return;

        for (Entity e : mc.world.getEntities()) {

            if (!(e instanceof LivingEntity))
                continue;

            if (e == mc.player)
                continue;

            // 3 blocks
            if (mc.player.squaredDistanceTo(e) > 9.0)
                continue;

            int axeSlot = findAxe(mc);

            if (axeSlot == -1) {

                send("No axe");

                return;
            }

            // swap
            mc.player.getInventory().setSelectedSlot(axeSlot);

            if (mc.getNetworkHandler() != null) {

                mc.getNetworkHandler().sendPacket(
                        new UpdateSelectedSlotC2SPacket(
                                axeSlot
                        )
                );
            }

            // attack
            if (mc.interactionManager != null) {

                mc.interactionManager.attackEntity(
                        mc.player,
                        e
                );

                mc.player.swingHand(Hand.MAIN_HAND);

                send("Triggered");
            }

            return;
        }
    }

    private int findAxe(MinecraftClient mc) {

        for (int i = 0; i < 9; i++) {

            if (mc.player.getInventory()
                    .getStack(i)
                    .getItem() instanceof AxeItem) {

                return i;
            }
        }

        return -1;
    }

    private void send(String text) {

        MinecraftClient mc =
                MinecraftClient.getInstance();

        if (mc.player == null)
            return;

        mc.player.sendMessage(
                net.minecraft.text.Text.literal(
                        "[AutoStun] " + text
                ),
                true
        );
    }
}
