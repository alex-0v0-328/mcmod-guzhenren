package net.alex.guzhenren.registry.damage;

import net.alex.guzhenren.Guzhenren;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.entity.Entity;
import org.jetbrains.annotations.Nullable;

/**
 * 本模组伤害类型的键；伤害源只经 {@code source} 的两个重载构建。
 *
 * <ul>
 *   <li>资源键容器：伤害类型在数据包注册表，只持 {@link ResourceKey}，JSON 由 datagen provider 写（运行时无）。</li>
 *   <li>⚠ 6 种天灾类带同样 6 个标签（5 个 {@code BYPASSES_*} 加 {@code NO_KNOCKBACK}）；寿元耗尽、魂魄衰竭、脑海炸裂、
 *       空窍[Aperture]压力爆炸另带 {@code BYPASSES_INVULNERABILITY}，不死图腾挡不住；空窍压力爆炸、十绝天灾另带
 *       {@code IS_EXPLOSION}。</li>
 *   <li>其余是普通战斗伤害：月刃、气波是护甲可挡的弹射物；流血如毒穿甲；范围冲击、蓄力一拳护甲可挡、
 *       无击退（{@code AreaImpact}、{@code ChargedPunch} 自行推开目标）；气波、蓄力一拳另绕过受伤冷却。</li>
 *   <li>肉体过载（{@code BodyOverload}）、冲击反噬（{@code ChargedPunch}）是拳对自身的反冲：穿甲、无击退、无攻击者、
 *       不致死。</li>
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
