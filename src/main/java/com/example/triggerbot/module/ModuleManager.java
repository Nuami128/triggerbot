package com.example.triggerbot.module;

import com.example.triggerbot.module.impl.AimAssistModule;
import com.example.triggerbot.module.impl.AutoJumpResetModule;
import com.example.triggerbot.module.impl.AutoShieldModule;
import com.example.triggerbot.module.impl.AutoSprintModule;
import com.example.triggerbot.module.impl.AutoStunModule;
import com.example.triggerbot.module.impl.InventoryEatModule;
import java.util.List;

public class ModuleManager {
    private final List<ClientModule> modules = List.of(
            new AimAssistModule(),
            new AutoJumpResetModule(),
            new AutoStunModule(),
            new AutoSprintModule(),
            new AutoShieldModule(),
            new InventoryEatModule()
    );

    public void initialize() {
        modules.forEach(ClientModule::onInitialize);
    }

    public List<ClientModule> getModules() {
        return modules;
    }
}
