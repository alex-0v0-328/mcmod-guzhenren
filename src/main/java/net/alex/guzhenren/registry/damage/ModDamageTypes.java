package net.alex.guzhenren.registry.damage;

import net.alex.guzhenren.Guzhenren;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.entity.Entity;
import org.jetbrains.annotations.Nullable;

/**
 * 本模组伤害类型的键，唯一工厂 {@code source(entity, type)}。
 *
 * <ul>
 *   <li>资源键容器：伤害类型在数据包注册表，只持 {@link ResourceKey}，JSON 由 datagen provider 写（运行时无）。</li>
 *   <li>⚠ 6 种天灾类带同样 6 个标签（5 个 {@code BYPASSES_*} 加 {@code NO_KNOCKBACK}），刻意不带
 *       {@code BYPASSES_INVULNERABILITY}，创造模式仍杀不死；空窍[Aperture]压力爆炸另带 {@code IS_EXPLOSION}。</li>
 *   <li>另 3 种普通战斗伤害：月刃，可挡的弹射物；流血，如毒穿甲；范围冲击，可挡，只带
 *       {@code NO_KNOCKBACK}（{@code AreaImpact} 随拳推开）。肉体过载：拳的自身反冲，穿甲、无攻击者、
 *       不致死（{@code BodyOverload}）。</li>
 * </ul>
 *
 * @author Alex
 * @version 1.0.0
 * @since 1.0.0
 */

public final class ModDamageTypes {

    private ModDamageTypes() {}

    public static final ResourceKey<DamageType> LIFESPAN_EXHAUSTED = key("lifespan_exhausted");
    public static final ResourceKey<DamageType> SOUL_COLLAPSE = key("soul_collapse");
    public static final ResourceKey<DamageType> MIND_OCEAN_SHATTERED = key("mind_ocean_shattered");
    public static final ResourceKey<DamageType> APERTURE_PRESSURE_EXPLOSION = key("aperture_pressure_explosion");
    public static final ResourceKey<DamageType> TEN_EXTREME_DISASTER = key("ten_extreme_disaster");
    public static final ResourceKey<DamageType> VITAL_GU_LOST = key("vital_gu_lost");
    public static final ResourceKey<DamageType> MOON_BLADE = key("moon_blade");
    public static final ResourceKey<DamageType> BLEEDING = key("bleeding");
    public static final ResourceKey<DamageType> AREA_IMPACT = key("area_impact");
    public static final ResourceKey<DamageType> QI_WAVE = key("qi_wave");
    public static final ResourceKey<DamageType> BODY_OVERLOAD = key("body_overload");
    public static final ResourceKey<DamageType> CHARGED_PUNCH = key("charged_punch");
    public static final ResourceKey<DamageType> IMPACT_BACKLASH = key("impact_backlash");

    public static DamageSource source(Entity entity, ResourceKey<DamageType> type) {
        return new DamageSource(entity.level().registryAccess()
                .registryOrThrow(Registries.DAMAGE_TYPE)
                .getHolderOrThrow(type));
    }

    public static DamageSource source(Entity direct, @Nullable Entity attacker, ResourceKey<DamageType> type) {
        return new DamageSource(direct.level().registryAccess()
                .registryOrThrow(Registries.DAMAGE_TYPE)
                .getHolderOrThrow(type), direct, attacker);
    }

    private static ResourceKey<DamageType> key(String name) {
        return ResourceKey.create(Registries.DAMAGE_TYPE,
                Guzhenren.id(name));
    }
}
