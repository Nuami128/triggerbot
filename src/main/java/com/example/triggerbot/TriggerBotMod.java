package com.example.triggerbot;

import com.example.triggerbot.module.ModuleManager;
import com.example.triggerbot.module.impl.AutoStunModule;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;

import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import org.lwjgl.glfw.GLFW;

public class TriggerBotMod implements ClientModInitializer {

    private static final ModuleManager MODULE_MANAGER = ModuleManager.getInstance();

    private static KeyBinding AUTOSTUN_KEY;

    @Override
    public void onInitializeClient() {

        // Register module
        MODULE_MANAGER.register(new AutoStunModule());

        // Register keybind (visible in Controls → Misc)
        AUTOSTUN_KEY = KeyBindingHelper.registerKeyBinding(
                new KeyBinding(
                        "key.triggerbot.autostun",
                        InputUtil.Type.KEYSYM,
                        GLFW.GLFW_KEY_R,
                        KeyBinding.Category.MISC
                )
        );

        // Main tick loop
        ClientTickEvents.END_CLIENT_TICK.register(client -> {

    while (AUTOSTUN_KEY.wasPressed()) {
        System.out.println("R WORKS");
    }

    MODULE_MANAGER.tickAll();
});

    public static ModuleManager getModuleManager() {
        return MODULE_MANAGER;
    }
}
