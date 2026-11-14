package net.alex.guzhenren.client.icon;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.effect.MobEffectInstance;

/**
 * 借授予该效果的物品图标绘制的效果图标，两者一眼对应。
 * <ul>
 *   <li>存物品注册 id 与预建的贴图位置；路径恒为 {@code item/<id>}，忽略 {@code amplifier}：非分级效果仅一张脸。</li>
 *   <li>⚠ 传物品注册 id 而非 {@code mob_effect} 贴图：效果沿用蛊图标就不必另画；此处缺图显示紫黑格，不回退。</li>
 * </ul>
 *
 * @author Alex
 * @version 1.0.0
 * @see net.alex.guzhenren.client.icon.EffectIconLayout
 * @see net.alex.guzhenren.client.icon.GradedEffectIcon
 * @since 1.0.0
 */

public final class ItemEffectIcon implements EffectIconLayout {

    private final String texture;
    private final ResourceLocation location;

    public ItemEffectIcon(String item) {
        this.texture = "item/" + item;
        this.location = EffectIconLayout.getLocationOf(texture);
    }

    @Override
    public String getTextureFor(MobEffectInstance instance) { return texture; }

    @Override
    public ResourceLocation getLocationFor(MobEffectInstance instance) { return location; }
}
