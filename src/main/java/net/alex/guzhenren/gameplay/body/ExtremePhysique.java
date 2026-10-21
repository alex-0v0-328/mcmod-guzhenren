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
 * The Ten-Extremes [十绝] physiques and the body tables that hang off each of them.
 *
 * <p>The concrete physique is stored on {@code BodyData}; the aperture keeps only the base-essence
 * value that identifies the top aptitude grade.
 *
 * <p>⚠ The birth die skips {@code PURE_DREAM_REALITY_SEEKER}: it translates and is settable but can
 * never be rolled.
 *
 * @author Alex
 * @version 1.0.0
 * @see GuPath
 * @since 1.0.0
 */

public enum ExtremePhysique implements NamedEnum {

    NONE(100, 0),
    VERDANT_GREAT_SUN(100, 50, GuPath.SPACE),
    DESOLATE_ANCIENT_MOON(100, 50, GuPath.TIME),
    NORTHERN_DARK_ICE_SOUL(100, 50, GuPath.ICE_SNOW, GuPath.SOUL),
    BOUNDLESS_FOREST_SAMSARA(100, 50, GuPath.WOOD),
    BLAZING_GLORY_LIGHTNING_BRILLIANCE(100, 50, GuPath.FIRE, GuPath.LIGHTNING),
    MYRIAD_GOLD_WONDROUS_ESSENCE(100, 50, GuPath.METAL),
    GREAT_STRENGTH_TRUE_MARTIAL(300, 150, GuPath.STRENGTH),
    CAREFREE_WISDOM_HEART(100, 50, GuPath.WISDOM),
    PROFOUND_EARTH_ORIGIN(100, 50, GuPath.EARTH),
    UNIVERSE_GREAT_DERIVATION(100, 50, GuPath.RULE),
    PURE_DREAM_REALITY_SEEKER(100, 50, GuPath.DREAM);

    public static final Codec<ExtremePhysique> CODEC = StringRepresentable.fromEnum(ExtremePhysique::values);
    private final int strengthCapacity;
    private final int staminaMaxPercent;
    private final List<GuPath> talentPaths;

    ExtremePhysique(int strengthCapacity, int staminaMaxPercent, GuPath... talentPaths) {
        this.strengthCapacity = strengthCapacity;
        this.staminaMaxPercent = staminaMaxPercent;
        this.talentPaths = List.of(talentPaths);
    }

    public int getStrengthCapacity() { return strengthCapacity; }

    public int getStaminaMaxPercent() { return staminaMaxPercent; }

    public List<GuPath> getTalentPaths() { return talentPaths; }

    public static ExtremePhysique[] settable() { return Arrays.copyOfRange(values(), 1, values().length); }

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
