package net.alex.guzhenren.datagen.damage;

import java.util.concurrent.CompletableFuture;
import net.alex.guzhenren.Guzhenren;
import net.alex.guzhenren.registry.damage.ModDamageTypes;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.PackOutput;
import net.minecraft.data.tags.TagsProvider;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.damagesource.DamageType;
import net.neoforged.neoforge.common.data.ExistingFileHelper;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * 伤害类型标签；取数据包 provider 的 lookup，才见本次生成的类型。
 *
 * <ul>
 *   <li>六种灾难伤害同带六个绕过标签：护甲、效果、附魔、抗性、盾牌、无击退；月刃只是弹射物；流血只绕过护甲。</li>
 *   <li>空窍压力爆炸、十绝天灾另带 {@code IS_EXPLOSION}；气波为弹射物、绕过受伤冷却：一拳的多个兽力冲击才各计伤
 *       害。</li>
 *   <li>肉体过载、冲击反噬绕过护甲且无击退：身体反冲无来源位置，1.21 会朝随机方向击退；范围冲击、蓄力一拳也无击退：各自
 *       推开目标。</li>
 *   <li>仅四种致死检查（寿元耗尽、魂魄衰竭、脑海炸裂、空窍压力爆炸）另带 {@code BYPASSES_INVULNERABILITY}，不死图腾挡不
 *       住；心跳先跳过创造与旁观玩家。</li>
 *   <li>十绝天灾、本命蛊死亡不带它：前者波及爆炸内全部实体，后者按比例扣血，带了只多打到创造玩家与无敌实体。</li>
 * </ul>
 *
 * @author Alex
 * @version 1.0.0
 * @see net.alex.guzhenren.datagen.ModDatapackProvider
 * @since 1.0.0
 */

public class ModDamageTypeTagsProvider extends TagsProvider<DamageType> {

    public ModDamageTypeTagsProvider(PackOutput output,
                                     CompletableFuture<HolderLookup.Provider> lookupProvider,
                                     @Nullable ExistingFileHelper existingFileHelper) {
        super(output, Registries.DAMAGE_TYPE, lookupProvider, Guzhenren.MOD_ID, existingFileHelper);
    }

    @Override
    protected void addTags(HolderLookup.@NotNull Provider provider) {
        tagUnstoppable(DamageTypeTags.BYPASSES_ARMOR);
        tagUnstoppable(DamageTypeTags.BYPASSES_EFFECTS);
        tagUnstoppable(DamageTypeTags.BYPASSES_ENCHANTMENTS);
        tagUnstoppable(DamageTypeTags.BYPASSES_RESISTANCE);
        tagUnstoppable(DamageTypeTags.BYPASSES_SHIELD);
        tagUnstoppable(DamageTypeTags.NO_KNOCKBACK);
        tag(DamageTypeTags.BYPASSES_INVULNERABILITY).add(ModDamageTypes.LIFESPAN_EXHAUSTED,
                ModDamageTypes.SOUL_COLLAPSE,
                ModDamageTypes.MIND_OCEAN_SHATTERED,
                ModDamageTypes.APERTURE_PRESSURE_EXPLOSION);
        tag(DamageTypeTags.IS_EXPLOSION).add(ModDamageTypes.APERTURE_PRESSURE_EXPLOSION,
                ModDamageTypes.TEN_EXTREME_DISASTER);
        tag(DamageTypeTags.IS_PROJECTILE).add(ModDamageTypes.MOON_BLADE, ModDamageTypes.QI_WAVE);
        tag(DamageTypeTags.BYPASSES_COOLDOWN).add(ModDamageTypes.QI_WAVE, ModDamageTypes.CHARGED_PUNCH);
        tag(DamageTypeTags.BYPASSES_ARMOR).add(ModDamageTypes.BLEEDING, ModDamageTypes.BODY_OVERLOAD,
                ModDamageTypes.IMPACT_BACKLASH);
        tag(DamageTypeTags.NO_KNOCKBACK).add(ModDamageTypes.AREA_IMPACT, ModDamageTypes.CHARGED_PUNCH,
                ModDamageTypes.BODY_OVERLOAD, ModDamageTypes.IMPACT_BACKLASH);
    }

    private void tagUnstoppable(TagKey<DamageType> tag) {
        tag(tag).add(ModDamageTypes.LIFESPAN_EXHAUSTED,
                ModDamageTypes.SOUL_COLLAPSE,
                ModDamageTypes.MIND_OCEAN_SHATTERED,
                ModDamageTypes.APERTURE_PRESSURE_EXPLOSION,
                ModDamageTypes.TEN_EXTREME_DISASTER,
                ModDamageTypes.VITAL_GU_LOST);
    }
}
