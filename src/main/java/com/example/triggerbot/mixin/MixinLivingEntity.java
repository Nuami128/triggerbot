package com.example.triggerbot.mixin;

// This mixin is intentionally left empty.
// The damage detection has been moved to AutoJumpResetModule.onTick()
// which watches hurtTime transitioning from 0 to max (10).
// This avoids targeting LivingEntity.damage() whose obfuscated name
// is unknown at runtime on Mojo (no refMap, no intermediary names).

import net.minecraft.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(LivingEntity.class)
public class MixinLivingEntity {
    // intentionally empty
}
