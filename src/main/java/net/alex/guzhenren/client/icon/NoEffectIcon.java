package net.alex.guzhenren.client.icon;

import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.EffectRenderingInventoryScreen;
import net.minecraft.world.effect.MobEffectInstance;
import net.neoforged.neoforge.client.extensions.common.IClientMobEffectExtensions;
import org.jetbrains.annotations.NotNull;

/**
 * An effect that wears no icon: it stays off the HUD, and the inventory lists its name and time with an empty
 * icon slot.
 *
 * <p>⚠ The inventory screen ignores the instance's {@code showIcon} flag and draws the effect's own sprite, which
 * a texture-less effect lacks, so without this the slot shows the missing-texture checkerboard.
 *
 * @author Alex
 * @version 1.0.0
 * @see net.alex.guzhenren.client.icon.EffectIcons
 * @since 1.0.0
 */

public final class NoEffectIcon implements IClientMobEffectExtensions {

    @Override
    public boolean isVisibleInGui(@NotNull MobEffectInstance instance) { return false; }

    @Override
    public boolean renderGuiIcon(@NotNull MobEffectInstance instance, @NotNull Gui gui, @NotNull GuiGraphics graphics,
                                 int x, int y, float z, float alpha) {
        return true;
    }

    @Override
    public boolean renderInventoryIcon(@NotNull MobEffectInstance instance,
                                       @NotNull EffectRenderingInventoryScreen<?> screen,
                                       @NotNull GuiGraphics graphics, int x, int y, int blitOffset) {
        return true;
    }
}
