package net.alex.guzhenren.client.icon;

import net.alex.guzhenren.Guzhenren;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.EffectRenderingInventoryScreen;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.effect.MobEffectInstance;
import net.neoforged.neoforge.client.extensions.common.IClientMobEffectExtensions;
import org.jetbrains.annotations.NotNull;

/**
 * Shared layout for an effect icon drawn from a texture: slot centering, the HUD/inventory render hooks,
 * and the one abstract seam -- which texture this instance wears.
 *
 * <p>Implements {@link net.neoforged.neoforge.client.extensions.common.IClientMobEffectExtensions}.
 * The two default render methods (HUD and inventory) both delegate to {@code locationFor}, so a new
 * effect icon strategy is one class implementing that seam, and it hands back a {@code ResourceLocation} it built
 * ahead of time, because the render methods run once per icon per frame. {@code textureFor} is the same texture as
 * a path string. All icons are 16×16, centered with a 1px inset.
 *
 * @author Alex
 * @version 1.0.0
 * @see net.alex.guzhenren.client.icon.GradedEffectIcon
 * @see net.alex.guzhenren.client.icon.ItemEffectIcon
 * @since 1.0.0
 */

public interface EffectIconLayout extends IClientMobEffectExtensions {

    int ICON_SIZE = 16;
    int CENTRE_IN_SLOT = 1;
    int HUD_SLOT_X = 3;
    int HUD_SLOT_Y = 3;
    int INVENTORY_SLOT_Y = 7;

    String textureFor(MobEffectInstance instance);

    ResourceLocation locationFor(MobEffectInstance instance);

    static ResourceLocation locationOf(String texture) { return Guzhenren.id("textures/" + texture + ".png"); }

    private void draw(GuiGraphics g, MobEffectInstance instance, int x, int y) {
        g.blit(locationFor(instance), x, y, 0, 0.0F, 0.0F, ICON_SIZE, ICON_SIZE, ICON_SIZE, ICON_SIZE);
    }

    @Override
    default boolean renderGuiIcon(@NotNull MobEffectInstance instance, @NotNull Gui gui, GuiGraphics graphics,
                                  int x, int y, float z, float alpha) {
        graphics.setColor(1.0F, 1.0F, 1.0F, alpha);
        draw(graphics, instance, x + HUD_SLOT_X + CENTRE_IN_SLOT, y + HUD_SLOT_Y + CENTRE_IN_SLOT);
        graphics.setColor(1.0F, 1.0F, 1.0F, 1.0F);
        return true;
    }

    @Override
    default boolean renderInventoryIcon(@NotNull MobEffectInstance instance,
                                        @NotNull EffectRenderingInventoryScreen<?> screen,
                                        @NotNull GuiGraphics graphics, int x, int y, int blitOffset) {
        draw(graphics, instance, x + CENTRE_IN_SLOT, y + INVENTORY_SLOT_Y + CENTRE_IN_SLOT);
        return true;
    }
}
