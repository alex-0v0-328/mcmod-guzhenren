package net.alex.guzhenren.client.icon;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.effect.MobEffectInstance;

/**
 * 直接借用对应物品图标绘制的状态效果图标：使玩家一眼即可把效果与授予该效果的蛊虫关联起来。
 * <ul>
 *   <li>仅保存物品注册 ID 与预建贴图位置，路径固定为 {@code item/<id>} 且忽略 {@code amplifier}（非分级效果只保留一
 *       张图标）。</li>
 *   <li>⚠ 此处刻意直接指向物品贴图而非 {@code mob_effect} 目录，以便沿用现有蛊图标而无需重复绘制；若对应物品缺少贴
 *       图将直接显示紫黑缺失纹理，不会自动回退。</li>
 * </ul>
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
