package net.alex.guzhenren.client.hud;

import net.alex.guzhenren.Guzhenren;
import net.alex.guzhenren.gameplay.aperture.Talent;
import net.alex.guzhenren.gameplay.aperture.awakening.AwakeningRitual;
import net.alex.guzhenren.gameplay.body.ExtremePhysique;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.EnumMap;
import java.util.Map;

/**
 * The aptitude plaque [资质牌匾] an awakening ritual [开窍仪式] reveals: a GUI sprite named after the rolled grade
 * ({@code plaque/talent/<grade>}), or after the physique for Ten Extremes ({@code plaque/extreme_physique/<physique>}),
 * {@link #WIDTH} pixels wide and {@code height} tall. Shared by the plaque on the player's own screen
 * ({@link AwakeningHud}) and the one over the head ({@code AwakeningPlaqueRenderer}).
 *
 * @author Alex
 * @version 1.0.0
 * @see AwakeningHud
 * @since 1.0.0
 */

public record AwakeningPlaque(@NotNull ResourceLocation sprite, int height) {

    public static final int WIDTH = 16;
    private static final int TALENT_HEIGHT = 52;
    private static final int PHYSIQUE_HEIGHT = 64;
    private static final Map<Talent, AwakeningPlaque> TALENTS = new EnumMap<>(Talent.class);
    private static final Map<ExtremePhysique, AwakeningPlaque> PHYSIQUES = new EnumMap<>(ExtremePhysique.class);

    static {
        for (Talent talent : Talent.getSettable()) {
            if (talent == Talent.TEN_EXTREMES) continue;
            TALENTS.put(talent, new AwakeningPlaque(
                    Guzhenren.id("plaque/talent/" + talent.getSerializedName()), TALENT_HEIGHT));
        }
        for (ExtremePhysique physique : ExtremePhysique.getSettable()) {
            PHYSIQUES.put(physique, new AwakeningPlaque(
                    Guzhenren.id("plaque/extreme_physique/" + physique.getSerializedName()), PHYSIQUE_HEIGHT));
        }
    }

    public static @Nullable AwakeningPlaque of(@NotNull AwakeningRitual ritual) {
        Talent talent = Talent.fromPercent(ritual.base());
        return talent == Talent.TEN_EXTREMES ? PHYSIQUES.get(ritual.physique()) : TALENTS.get(talent);
    }
}
