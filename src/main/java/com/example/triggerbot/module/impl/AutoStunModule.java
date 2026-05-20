package com.example.triggerbot.module.impl;

import com.example.triggerbot.module.ClientModule;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;

import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;

import net.minecraft.entity.player.PlayerInventory;

import net.minecraft.item.AxeItem;
import net.minecraft.item.ItemStack;

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

    private enum Stage {
        IDLE,
        SWAP,
        ATTACK,
        RETURN
    }

    private Stage stage = Stage.IDLE;

    private Entity currentTarget = null;

    private int savedSlot = -1;

    private long lastActionTime = 0L;

    private static final long DELAY_MS = 50L;

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

        reset();

        send("Disabled");
    }

    @Override
    public void onTick() {

        if (!enabled)
            return;

        MinecraftClient mc = MinecraftClient.getInstance();

        if (mc.player == null || mc.world == null)
            return;

        // don't interrupt eating/shielding
        if (mc.player.isUsingItem())
            return;

        // only while attacking
        if (!mc.options.attackKey.isPressed())
            return;

        // attack cooldown
        if (mc.player.getAttackCooldownProgress(0.5f) < 0.92f)
            return;

        // ─────────────────────────────
        // IDLE
        // ─────────────────────────────

        if (stage == Stage.IDLE) {

            Entity target = findCrosshairTarget(mc);

            if (target == null)
                return;

            currentTarget = target;

            stage = Stage.SWAP;

            lastActionTime = System.currentTimeMillis();

            return;
        }

        // ─────────────────────────────
        // SWAP
        // ─────────────────────────────

        if (stage == Stage.SWAP) {

            if (!passedDelay())
                return;

            savedSlot =
                    mc.player.getInventory().getSelectedSlot();

            int axeSlot =
                    findHotbarSlot(
                            mc.player.getInventory(),
                            AxeItem.class
                    );

            if (axeSlot == -1) {

                send("No axe found");

                reset();

                return;
            }

            swap(mc, axeSlot);

            stage = Stage.ATTACK;

            lastActionTime = System.currentTimeMillis();

            return;
        }

        // ─────────────────────────────
        // ATTACK
        // ─────────────────────────────

        if (stage == Stage.ATTACK) {

            if (!passedDelay())
                return;

            Entity confirm =
                    findCrosshairTarget(mc);

            // target moved / invalid
            if (confirm == null
                    || confirm != currentTarget) {

                reset();

                return;
            }

            attack(mc, confirm);

            stage = Stage.RETURN;

            lastActionTime = System.currentTimeMillis();

            return;
        }

        // ─────────────────────────────
        // RETURN
        // ─────────────────────────────

        if (stage == Stage.RETURN) {

            if (!passedDelay())
                return;

            if (savedSlot != -1) {

                swap(mc, savedSlot);
            }

            reset();
        }
    }

    // ─────────────────────────────
    // TARGETING
    // ─────────────────────────────

    private Entity findCrosshairTarget(
            MinecraftClient mc
    ) {

        Entity bestTarget = null;

        double bestAngle = 999.0;

        for (Entity e : mc.world.getEntities()) {

            if (!(e instanceof LivingEntity living))
                continue;

            if (e == mc.player)
                continue;

            if (!e.isAlive())
                continue;

            // must be shielding
            if (!living.isBlocking())
                continue;

            // 3 block range
            if (mc.player.squaredDistanceTo(e) > 9.0)
                continue;

            double angle =
                    getAngleToEntity(mc, e);

            // soft crosshair alignment
            if (angle > 10.0)
                continue;

            if (angle < bestAngle) {

                bestAngle = angle;

                bestTarget = e;
            }
        }

        return bestTarget;
    }

    private double getAngleToEntity(
            MinecraftClient mc,
            Entity entity
    ) {

        double dx =
                entity.getX() - mc.player.getX();

        double dy =
                (entity.getY()
                        + entity.getHeight() * 0.5)
                        - (mc.player.getY()
                        + mc.player.getEyeHeight(
                        mc.player.getPose()));

        double dz =
                entity.getZ() - mc.player.getZ();

        double distance =
                Math.sqrt(dx * dx + dz * dz);

        float targetYaw =
                (float)Math.toDegrees(
                        Math.atan2(dz, dx))
                        - 90.0F;

        float targetPitch =
                (float)-Math.toDegrees(
                        Math.atan2(dy, distance));

        float yawDiff =
                Math.abs(
                        wrapDegrees(
                                mc.player.getYaw()
                                        - targetYaw
                        )
                );

        float pitchDiff =
                Math.abs(
                        wrapDegrees(
                                mc.player.getPitch()
                                        - targetPitch
                        )
                );

        return Math.sqrt(
                yawDiff * yawDiff
                        + pitchDiff * pitchDiff
        );
    }

    private float wrapDegrees(float degrees) {

        degrees = degrees % 360.0F;

        if (degrees >= 180.0F) {
            degrees -= 360.0F;
        }

        if (degrees < -180.0F) {
            degrees += 360.0F;
        }

        return degrees;
    }

    // ─────────────────────────────
    // ACTIONS
    // ─────────────────────────────

    private void attack(
            MinecraftClient mc,
            Entity target
    ) {

        if (mc.interactionManager == null)
            return;

        mc.interactionManager.attackEntity(
                mc.player,
                target
        );

        mc.player.swingHand(Hand.MAIN_HAND);

        send("Triggered");
    }

    private void swap(
            MinecraftClient mc,
            int slot
    ) {

        mc.player
                .getInventory()
                .setSelectedSlot(slot);

        if (mc.getNetworkHandler() != null) {

            mc.getNetworkHandler().sendPacket(
                    new UpdateSelectedSlotC2SPacket(
                            slot
                    )
            );
        }
    }

    // ─────────────────────────────
    // HELPERS
    // ─────────────────────────────

    private boolean passedDelay() {

        return System.currentTimeMillis()
                - lastActionTime
                >= DELAY_MS;
    }

    private void reset() {

        stage = Stage.IDLE;

        currentTarget = null;
    }

    private <T> int findHotbarSlot(
            PlayerInventory inv,
            Class<T> itemClass
    ) {

        for (int i = 0; i < 9; i++) {

            ItemStack stack = inv.getStack(i);

            if (!stack.isEmpty()
                    && itemClass.isInstance(
                    stack.getItem())) {

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
