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

    // ✅ Proper keybind stored in main client class
    private static KeyBinding AUTOSTUN_KEY;

    @Override
    public void onInitializeClient() {

        // Register modules
        MODULE_MANAGER.register(new AutoStunModule());

        // ✅ Register keybind correctly (shows in Controls menu)
        AUTOSTUN_KEY = KeyBindingHelper.registerKeyBinding(
                new KeyBinding(
                        "key.triggerbot.autostun",
                        InputUtil.Type.KEYSYM,
                        GLFW.GLFW_KEY_R,
                        "category.triggerbot"
                )
        );

        // Main tick loop
        ClientTickEvents.END_CLIENT_TICK.register(client -> {

            // Handle key press
            while (AUTOSTUN_KEY.wasPressed()) {

                AutoStunModule mod =
                        (AutoStunModule) MODULE_MANAGER
                                .find("AutoStun")
                                .orElse(null);

                if (mod == null) return;

                // Toggle + trigger behavior
                if (!mod.isEnabled()) {
                    mod.onEnable();
                    return;
                }

                mod.trigger();
            }

            // Tick modules normally
            MODULE_MANAGER.tickAll();
        });
    }

    public static ModuleManager getModuleManager() {
        return MODULE_MANAGER;
    }
}
