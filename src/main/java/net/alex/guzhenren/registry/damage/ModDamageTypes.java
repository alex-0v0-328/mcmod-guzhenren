package net.alex.guzhenren.registry.damage;

import net.alex.guzhenren.Guzhenren;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.entity.Entity;
import org.jetbrains.annotations.Nullable;

/**
 * 本模组全部数据包伤害类型的 {@link ResourceKey} 定义与 {@link DamageSource} 构建入口（对应 JSON 由 datagen provider 生
 * 成）。
 * <ul>
 *   <li>常规战斗与反冲伤害中，月刃、气波为可被护甲减免的弹射物伤害，流血如同中毒无视护甲；范围冲击、蓄力一拳与终点破坏
 *       打击可被护甲减免且无原版击退（由 {@code AreaImpact}、{@code ChargedPunch} 与 {@code CrashStrike} 自行施加击
 *       退），其中气波与蓄力一拳额外绕过受伤无敌帧；肉体过载与冲击反噬则是拳击对自身施加的穿甲、无击退、无攻击者且不致
 *       死反冲伤害。</li>
 *   <li>⚠ 6 种天灾与死劫型伤害均配置相同的 6 个绕过标签（5 个 {@code BYPASSES_*} 加 {@code NO_KNOCKBACK}）；其中寿元耗
 *       尽、魂魄衰竭、脑海炸裂与空窍压力爆炸额外携带 {@code BYPASSES_INVULNERABILITY} 使其无视不死图腾，空窍压力爆炸与
 *       十绝天灾额外携带 {@code IS_EXPLOSION}。</li>
 * </ul>
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
    public static final ResourceKey<DamageType> CRASH_STRIKE = key("crash_strike");

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
