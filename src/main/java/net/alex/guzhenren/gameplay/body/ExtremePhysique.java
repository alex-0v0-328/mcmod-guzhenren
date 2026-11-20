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
 * 十绝体[Ten Extreme Physiques]枚举：定义各十绝体的基础承受力起点、每转增量与天赋流派。
 * <ul>
 *   <li>具体体质存于 {@link BodyData#extremePhysique}，空窍仅存储代表十绝天资的满值基础真元。</li>
 *   <li>基础承受力 = {@link #getUnawakenedCapacity} + {@link #getCapacityPerRank} × 转数序号
 *       （{@code StrengthService.getBaseCapacity}）：凡体 12 起每转 +12，普通十绝体 36 起每转 +24，大力真武体 72 起每
 *       转 +36；筋骨皮淬炼与兽力的承受力加成另行加算。</li>
 *   <li>⚠ {@link #randomTenExtreme} 随机抽取十绝体时刻意跳过纯梦求真体（{@link #PURE_DREAM_REALITY_SEEKER}）：该体质保
 *       留翻译与指令设置支持，但永不由开窍或重掷自然抽出。</li>
 * </ul>
 */

public enum ExtremePhysique implements NamedEnum {

    NONE(12, 12),
    VERDANT_GREAT_SUN(36, 24, GuPath.SPACE),
    DESOLATE_ANCIENT_MOON(36, 24, GuPath.TIME),
    NORTHERN_DARK_ICE_SOUL(36, 24, GuPath.ICE_SNOW, GuPath.SOUL),
    BOUNDLESS_FOREST_SAMSARA(36, 24, GuPath.WOOD),
    BLAZING_GLORY_LIGHTNING_BRILLIANCE(36, 24, GuPath.FIRE, GuPath.LIGHTNING),
    MYRIAD_GOLD_WONDROUS_ESSENCE(36, 24, GuPath.METAL),
    GREAT_STRENGTH_TRUE_MARTIAL(72, 36, GuPath.STRENGTH),
    CAREFREE_WISDOM_HEART(36, 24, GuPath.WISDOM),
    PROFOUND_EARTH_ORIGIN(36, 24, GuPath.EARTH),
    UNIVERSE_GREAT_DERIVATION(36, 24, GuPath.RULE),
    PURE_DREAM_REALITY_SEEKER(36, 24, GuPath.DREAM);

    public static final Codec<ExtremePhysique> CODEC = StringRepresentable.fromEnum(ExtremePhysique::values);
    private final int unawakenedCapacity;
    private final int capacityPerRank;
    private final List<GuPath> talentPaths;

    ExtremePhysique(int unawakenedCapacity, int capacityPerRank, GuPath... talentPaths) {
        this.unawakenedCapacity = unawakenedCapacity;
        this.capacityPerRank = capacityPerRank;
        this.talentPaths = List.of(talentPaths);
    }

    public int getUnawakenedCapacity() { return unawakenedCapacity; }

    public int getCapacityPerRank() { return capacityPerRank; }

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
