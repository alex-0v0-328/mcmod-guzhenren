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
 * 开窍[Awakening]仪式揭晓的资质[Aptitude]牌匾：GUI 精灵图按掷出的资质档命名（{@code plaque/talent/<grade>}），十绝天资
 * 按体质命名（{@code plaque/extreme_physique/<physique>}）；宽 {@link #WIDTH} 像素、高 {@code height}；本地屏幕
 * （{@link AwakeningHud}）与头顶（{@code AwakeningPlaqueRenderer}）的牌匾共用。
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
