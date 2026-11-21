package net.alex.guzhenren.registry.entity;

import net.alex.guzhenren.Guzhenren;
import net.minecraft.core.registries.Registries;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.EntityType;

/**
 * 本模组实体类型标签键容器：{@link #PREDATOR_PREY} 指定猎食野兽（亚洲黑熊与老虎）主动攻击的原版家畜与小动物（本模组生物
 * 之间互不主动攻击）；{@link #WILD_GU} 归集全部 {@code WildGuEntity} 子类型，供范围打击与飞行冲撞判定豁免而无需在代码上
 * 反向依赖 {@code entity} 包；{@link #SOULSUNDER_IMMUNE} 收录荡魂山[Soulsunder Mountain]不扣血的实体（盔甲架）。
 */

public final class ModEntityTypeTags {

    private ModEntityTypeTags() {}

    public static final TagKey<EntityType<?>> PREDATOR_PREY = TagKey.create(
            Registries.ENTITY_TYPE, Guzhenren.id("predator_prey"));
    public static final TagKey<EntityType<?>> WILD_GU = TagKey.create(
            Registries.ENTITY_TYPE, Guzhenren.id("wild_gu"));
    public static final TagKey<EntityType<?>> SOULSUNDER_IMMUNE = TagKey.create(
            Registries.ENTITY_TYPE, Guzhenren.id("soulsunder_immune"));
}
