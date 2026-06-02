package com.example.triggerbot.mixin;

import net.minecraft.network.packet.s2c.play.EntityVelocityUpdateS2CPacket;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(EntityVelocityUpdateS2CPacket.class)
public interface EntityVelocityUpdateS2CPacketAccessor {
    @Accessor("velocityX") int getVelocityX();
    @Accessor("velocityY") int getVelocityY();
    @Accessor("velocityZ") int getVelocityZ();
}
