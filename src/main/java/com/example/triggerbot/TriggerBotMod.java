package com.example.triggerbot;

import net.fabricmc.api.ClientModInitializer;

import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;

import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;

import org.lwjgl.glfw.GLFW;

public class TriggerBotMod implements ClientModInitializer {

    private static KeyBinding TEST_KEY;

    @Override
    public void onInitializeClient() {

        System.out.println("TRIGGERBOT INIT");

        TEST_KEY = KeyBindingHelper.registerKeyBinding(
                new KeyBinding(
                        "key.triggerbot.test",
                        InputUtil.Type.KEYSYM,
                        GLFW.GLFW_KEY_R,
                        "key.categories.misc"
                )
        );

        ClientTickEvents.END_CLIENT_TICK.register(client -> {

            if (TEST_KEY.isPressed()) {

                if (client.player != null) {

                    client.player.sendMessage(
                            net.minecraft.text.Text.literal("R IS WORKING"),
                            true
                    );
                }
            }
        });
    }
}
