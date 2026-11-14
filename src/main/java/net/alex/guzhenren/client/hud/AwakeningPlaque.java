package net.alex.guzhenren.client.hud;

import com.mojang.blaze3d.platform.NativeImage;
import net.alex.guzhenren.Guzhenren;
import net.alex.guzhenren.gameplay.aperture.Talent;
import net.alex.guzhenren.gameplay.aperture.awakening.AwakeningRitual;
import net.alex.guzhenren.gameplay.body.ExtremePhysique;
import net.minecraft.client.renderer.texture.SpriteContents;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FastColor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.EnumMap;
import java.util.Map;
import java.util.WeakHashMap;

/**
 * 开窍[Awakening]仪式揭晓的资质[Aptitude]牌匾：GUI 精灵图按掷出的资质档命名（{@code plaque/talent/<grade>}），十绝天资
 * 按体质命名（{@code plaque/extreme_physique/<physique>}）；宽 {@link #WIDTH} 像素、高 {@code height}；本地屏幕
 * （{@link AwakeningHud}）与头顶（{@code AwakeningPlaqueRenderer}）的牌匾共用。
 * <ul>
 *   <li>{@link #getHighlight} 标出扫光可盖的像素：非底色（{@code #1f191b}、{@code #0c090a}）的不透明像素；按图集精灵内容
 *       缓存，资源重载换新内容即重读。</li>
 * </ul>
 *
 * @author Alex
 * @version 1.0.0
 * @see AwakeningHud
 * @since 1.0.0
 */

public record AwakeningPlaque(@NotNull ResourceLocation sprite, int height) {

    public static final int WIDTH = 16;
    public static final int TALENT_HEIGHT = 52;
    private static final int PHYSIQUE_HEIGHT = 64;
    private static final Map<Talent, AwakeningPlaque> TALENTS = new EnumMap<>(Talent.class);
    private static final Map<ExtremePhysique, AwakeningPlaque> PHYSIQUES = new EnumMap<>(ExtremePhysique.class);
    private static final int[] BASE_COLORS = {0x1F191B, 0x0C090A};
    private static final Map<SpriteContents, boolean[]> HIGHLIGHTS = new WeakHashMap<>();

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

    public static boolean @NotNull [] getHighlight(@NotNull TextureAtlasSprite sprite) {
        return HIGHLIGHTS.computeIfAbsent(sprite.contents(), AwakeningPlaque::readHighlight);
    }

    private static boolean[] readHighlight(SpriteContents contents) {
        NativeImage image = contents.getOriginalImage();
        int width = contents.width();
        boolean[] lit = new boolean[width * contents.height()];
        for (int y = 0; y < contents.height(); y++) {
            for (int x = 0; x < width; x++) {
                int abgr = image.getPixelRGBA(x, y);
                int rgb = FastColor.ABGR32.red(abgr) << 16 | FastColor.ABGR32.green(abgr) << 8
                        | FastColor.ABGR32.blue(abgr);
                lit[y * width + x] = FastColor.ABGR32.alpha(abgr) > 0 && rgb != BASE_COLORS[0] && rgb != BASE_COLORS[1];
            }
        }
        return lit;
    }
}
