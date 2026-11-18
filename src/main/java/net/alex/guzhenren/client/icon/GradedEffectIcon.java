package net.alex.guzhenren.client.icon;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.effect.MobEffectInstance;

/**
 * 按 {@code amplifier} 切换贴图的分级效果图标：同一效果的不同等级各自对应独立图标贴图。
 * <ul>
 *   <li>记录名称、贴图目录（{@code mob_effect} 或 {@code item}）与转数范围；拼接路径 {@code directory/name_rank}，
 *       其中转数由 {@code amplifier + amplifierOffset} 钳制到区间内得出（默认偏移为 1，仅起始转数高于一转的家族需要
 *       调整，例如恶念蛊[Malicious Thought Gu]从二转起算）；各转贴图位置在构造时预先建立，每帧渲染只做数组索引。</li>
 *   <li>⚠ 同一家族各等级仍必须保持为单个状态效果注册项：若按等级拆分为多个独立效果，会导致两个等级同时生效叠加。</li>
 * </ul>
 */

public final class GradedEffectIcon implements EffectIconLayout {

    private final String name;
    private final String directory;
    private final int lowestRank;
    private final int highestRank;
    private final int amplifierOffset;
    private final ResourceLocation[] locations;

    private GradedEffectIcon(String name, String directory, int lowestRank, int highestRank, int amplifierOffset) {
        this.name = name;
        this.directory = directory;
        this.lowestRank = lowestRank;
        this.highestRank = highestRank;
        this.amplifierOffset = amplifierOffset;
        this.locations = new ResourceLocation[highestRank - lowestRank + 1];
        for (int rank = lowestRank; rank <= highestRank; rank++) {
            locations[rank - lowestRank] = EffectIconLayout.getLocationOf(getTexture(rank));
        }
    }

    public static GradedEffectIcon createMobEffect(String name, int lowestRank, int highestRank) {
        return new GradedEffectIcon(name, "mob_effect", lowestRank, highestRank, 1);
    }

    public static GradedEffectIcon createItem(String name, int lowestRank, int highestRank) {
        return new GradedEffectIcon(name, "item", lowestRank, highestRank, 1);
    }

    public static GradedEffectIcon createItem(String name, int lowestRank, int highestRank, int amplifierOffset) {
        return new GradedEffectIcon(name, "item", lowestRank, highestRank, amplifierOffset);
    }

    @Override
    public String getTextureFor(MobEffectInstance instance) { return getTexture(getRankOf(instance)); }

    @Override
    public ResourceLocation getLocationFor(MobEffectInstance instance) {
        return locations[getRankOf(instance) - lowestRank];
    }

    private int getRankOf(MobEffectInstance instance) {
        return Mth.clamp(instance.getAmplifier() + amplifierOffset, lowestRank, highestRank);
    }

    private String getTexture(int rank) { return directory + "/" + name + "_" + rank; }
}
