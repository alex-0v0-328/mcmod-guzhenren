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
 * 由贴图绘制的效果图标共用布局：格内居中、HUD 与物品栏渲染钩子。
 * <ul>
 *   <li>唯一抽象接缝是用哪张贴图：两个默认渲染方法都委托 {@code locationFor}，新图标策略只需一个类实现它；它返回预建的
 *       {@code ResourceLocation}，因渲染方法每帧每图标调一次；{@code textureFor} 为同一贴图的路径串。</li>
 *   <li>图标一律 16×16，居中内缩 1 像素。</li>
 * </ul>
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

    String getTextureFor(MobEffectInstance instance);

    ResourceLocation getLocationFor(MobEffectInstance instance);

    static ResourceLocation getLocationOf(String texture) { return Guzhenren.id("textures/" + texture + ".png"); }

    private void draw(GuiGraphics g, MobEffectInstance instance, int x, int y) {
        g.blit(getLocationFor(instance), x, y, 0, 0.0F, 0.0F, ICON_SIZE, ICON_SIZE, ICON_SIZE, ICON_SIZE);
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
