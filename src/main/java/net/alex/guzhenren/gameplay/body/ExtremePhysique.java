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
 * 十绝体质及其肉身数值表。
 *
 * <ul>
 *   <li>具体体质存 {@code BodyData}；空窍[Aperture]只存标识最高资质[Aptitude]档的基础真元[Essence]值。</li>
 *   <li>{@link #getCapacityMultiplier} 缩放该转基础承受力（{@code StrengthService.getBaseCapacity}）：凡体 1，十绝体
 *       2，大力真武体 4；不影响淬炼所加。</li>
 *   <li>⚠ 出生骰跳过 {@code PURE_DREAM_REALITY_SEEKER}：有翻译、可设置，永不掷出。</li>
 * </ul>
 *
 * @author Alex
 * @version 1.0.0
 * @see GuPath
 * @since 1.0.0
 */

public enum ExtremePhysique implements NamedEnum {

    NONE(1, 0),
    VERDANT_GREAT_SUN(2, 50, GuPath.SPACE),
    DESOLATE_ANCIENT_MOON(2, 50, GuPath.TIME),
    NORTHERN_DARK_ICE_SOUL(2, 50, GuPath.ICE_SNOW, GuPath.SOUL),
    BOUNDLESS_FOREST_SAMSARA(2, 50, GuPath.WOOD),
    BLAZING_GLORY_LIGHTNING_BRILLIANCE(2, 50, GuPath.FIRE, GuPath.LIGHTNING),
    MYRIAD_GOLD_WONDROUS_ESSENCE(2, 50, GuPath.METAL),
    GREAT_STRENGTH_TRUE_MARTIAL(4, 150, GuPath.STRENGTH),
    CAREFREE_WISDOM_HEART(2, 50, GuPath.WISDOM),
    PROFOUND_EARTH_ORIGIN(2, 50, GuPath.EARTH),
    UNIVERSE_GREAT_DERIVATION(2, 50, GuPath.RULE),
    PURE_DREAM_REALITY_SEEKER(2, 50, GuPath.DREAM);

    public static final Codec<ExtremePhysique> CODEC = StringRepresentable.fromEnum(ExtremePhysique::values);
    private final int capacityMultiplier;
    private final int staminaMaxPercent;
    private final List<GuPath> talentPaths;

    ExtremePhysique(int capacityMultiplier, int staminaMaxPercent, GuPath... talentPaths) {
        this.capacityMultiplier = capacityMultiplier;
        this.staminaMaxPercent = staminaMaxPercent;
        this.talentPaths = List.of(talentPaths);
    }

    public int getCapacityMultiplier() { return capacityMultiplier; }

    public int getStaminaMaxPercent() { return staminaMaxPercent; }

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
