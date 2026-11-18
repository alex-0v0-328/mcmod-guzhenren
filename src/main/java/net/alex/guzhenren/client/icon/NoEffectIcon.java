package net.alex.guzhenren.client.icon;

import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.EffectRenderingInventoryScreen;
import net.minecraft.world.effect.MobEffectInstance;
import net.neoforged.neoforge.client.extensions.common.IClientMobEffectExtensions;
import org.jetbrains.annotations.NotNull;

/**
 * 无图标状态效果的空白占位渲染器：不在 HUD 中显示，物品栏列表中仅保留名称与剩余时长而将图标格留空。
 * <ul>
 *   <li>⚠ 原版物品栏界面会无视效果实例的 {@code showIcon} 标记并强行绘制效果自身贴图；对于本身不配贴图的隐藏或状态型效
 *       果，若不挂载本类占位则会直接暴露紫黑缺失纹理。</li>
 * </ul>
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
