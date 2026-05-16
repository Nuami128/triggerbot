package com.example.triggerbot.module;

public abstract class EmptyModule implements ClientModule {
    private final String name;

    protected EmptyModule(String name) {
        this.name = name;
    }

    @Override
    public String getName() {
        return name;
    }
}
