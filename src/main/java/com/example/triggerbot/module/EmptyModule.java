package com.example.triggerbot.module;

public abstract class EmptyModule implements ClientModule {

    private final String name;
    protected boolean enabled = false;

    protected EmptyModule(String name) {
        this.name = name;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public boolean isEnabled() {
        return enabled;
    }

    @Override
    public void onEnable() {
        enabled = true;
    }

    @Override
    public void onDisable() {
        enabled = false;
    }

    @Override
    public void onTick() {
        // default empty
    }
}
