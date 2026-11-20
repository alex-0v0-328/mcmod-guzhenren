package net.alex.guzhenren.gameplay.body;

import com.mojang.serialization.Codec;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import net.alex.guzhenren.core.NamedEnum;
import net.alex.guzhenren.gameplay.path.GuPath;
import net.minecraft.util.StringRepresentable;

/**
 * 十绝体[Ten Extreme Physiques]枚举：定义各十绝体的肉身承受力倍率与天赋流派。
 * <ul>
 *   <li>具体体质存于 {@link BodyData#extremePhysique}，空窍仅存储代表十绝天资的满值基础真元。</li>
 *   <li>{@link #getCapacityMultiplier} 仅缩放玩家当前转数的基础承受力（{@code StrengthService.getBaseCapacity}：凡体
 *       {@code 1}、普通十绝体 {@code 2}、大力真武体 {@code 4}），不放大筋骨皮淬炼提供的固定承受力加成。</li>
 *   <li>⚠ {@link #randomTenExtreme} 随机抽取十绝体时刻意跳过纯梦求真体（{@link #PURE_DREAM_REALITY_SEEKER}）：该体质保
 *       留翻译与指令设置支持，但永不由开窍或重掷自然抽出。</li>
 * </ul>
 */

public enum ExtremePhysique implements NamedEnum {

    NONE(1),
    VERDANT_GREAT_SUN(2, GuPath.SPACE),
    DESOLATE_ANCIENT_MOON(2, GuPath.TIME),
    NORTHERN_DARK_ICE_SOUL(2, GuPath.ICE_SNOW, GuPath.SOUL),
    BOUNDLESS_FOREST_SAMSARA(2, GuPath.WOOD),
    BLAZING_GLORY_LIGHTNING_BRILLIANCE(2, GuPath.FIRE, GuPath.LIGHTNING),
    MYRIAD_GOLD_WONDROUS_ESSENCE(2, GuPath.METAL),
    GREAT_STRENGTH_TRUE_MARTIAL(4, GuPath.STRENGTH),
    CAREFREE_WISDOM_HEART(2, GuPath.WISDOM),
    PROFOUND_EARTH_ORIGIN(2, GuPath.EARTH),
    UNIVERSE_GREAT_DERIVATION(2, GuPath.RULE),
    PURE_DREAM_REALITY_SEEKER(2, GuPath.DREAM);

    public static final Codec<ExtremePhysique> CODEC = StringRepresentable.fromEnum(ExtremePhysique::values);
    private final int capacityMultiplier;
    private final List<GuPath> talentPaths;

    ExtremePhysique(int capacityMultiplier, GuPath... talentPaths) {
        this.capacityMultiplier = capacityMultiplier;
        this.talentPaths = List.of(talentPaths);
    }

    public int getCapacityMultiplier() { return capacityMultiplier; }

    public List<GuPath> getTalentPaths() { return talentPaths; }

    public static ExtremePhysique[] getSettable() { return Arrays.copyOfRange(values(), 1, values().length); }

    public static ExtremePhysique randomTenExtreme() {
        List<ExtremePhysique> pool = new ArrayList<>();
        for (ExtremePhysique p : values()) {
            if (p != NONE && p != PURE_DREAM_REALITY_SEEKER) pool.add(p);
        }
        return pool.get(ThreadLocalRandom.current().nextInt(pool.size()));
    }

    @Override
    public String getTranslationPrefix() { return "guzhenren.enum.body.extreme_physique."; }
}
