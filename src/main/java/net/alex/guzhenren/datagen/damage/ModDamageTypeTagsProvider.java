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
 * 本模组伤害类型标签数据生成器（依赖 {@link net.alex.guzhenren.datagen.ModDatapackProvider#getRegistryProvider()} 解析
 * 当次生成的伤害类型）。
 * <ul>
 *   <li>六种灾劫与反噬致死伤害（寿元耗尽、魂魄衰竭、脑海炸裂、空窍压力爆炸、十绝天灾、本命蛊死亡）统一加入穿透护甲、效
 *       果、附魔、抗性、盾牌及无击退六个标签；其中仅前四种自身极限致死伤害额外加入
 *       {@link DamageTypeTags#BYPASSES_INVULNERABILITY} 以穿透不死图腾（心跳检查已提前跳过创造与旁观模式玩家，而十绝天
 *       灾与本命蛊死亡不加该标签以免误伤创造模式玩家或无敌实体）。</li>
 *   <li>空窍压力爆炸与十绝天灾加入爆炸标签；月刃与气波加入弹射物标签，且气波与蓄力一拳加入绕过受击无敌帧标签
 *       （{@link DamageTypeTags#BYPASSES_COOLDOWN}），使单拳触发的多道兽力气波能全额结算伤害。</li>
 *   <li>流血、肉体过载与冲击反噬加入穿透护甲标签；范围冲击、蓄力一拳、冲撞打击、肉体过载与冲击反噬均加入无击退标签（前
 *       三者由技能自身控制击退位移，后两者为无来源坐标的体内反冲，若不加无击退标签会被 1.21 原版朝随机水平方向击
 *       退）。</li>
 * </ul>
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
                ModDamageTypes.BODY_OVERLOAD, ModDamageTypes.IMPACT_BACKLASH, ModDamageTypes.CRASH_STRIKE);
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
