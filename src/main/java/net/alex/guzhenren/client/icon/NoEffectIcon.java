package net.alex.guzhenren.client.icon;

import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.EffectRenderingInventoryScreen;
import net.minecraft.world.effect.MobEffectInstance;
import net.neoforged.neoforge.client.extensions.common.IClientMobEffectExtensions;
import org.jetbrains.annotations.NotNull;

/**
 * 不带图标的效果：不上 HUD，物品栏列名称与时长，图标格留空。
 * <ul>
 *   <li>⚠ 物品栏无视实例的 {@code showIcon}，照画效果自身贴图；无贴图的效果缺这张图，不用本类则显示紫黑缺失贴图。</li>
 * </ul>
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
