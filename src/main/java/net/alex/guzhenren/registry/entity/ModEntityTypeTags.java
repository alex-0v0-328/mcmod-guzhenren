package net.alex.guzhenren.registry.entity;

import net.alex.guzhenren.Guzhenren;
import net.minecraft.core.registries.Registries;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.EntityType;

/**
 * 本模组声明的实体类型标签。
 *
 * <ul>
 *   <li>{@link #PREDATOR_PREY}：猎食野兽（亚洲黑熊、老虎）主动攻击的原版家畜与小动物；本模组生物互不主动攻击。</li>
 *   <li>{@link #WILD_GU}：全部 {@code WildGuEntity} 子类型，范围打击、飞行冲撞不中的小蛊；查标签免得
 *       {@code instanceof} 上依赖 {@code entity}。</li>
 * </ul>
 */

public final class ModEntityTypeTags {

    private ModEntityTypeTags() {}

    public static final TagKey<EntityType<?>> PREDATOR_PREY = TagKey.create(
            Registries.ENTITY_TYPE, Guzhenren.id("predator_prey"));
    public static final TagKey<EntityType<?>> WILD_GU = TagKey.create(
            Registries.ENTITY_TYPE, Guzhenren.id("wild_gu"));
}
