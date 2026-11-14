package net.alex.guzhenren.gameplay.body;

import com.mojang.serialization.Codec;
import net.alex.guzhenren.core.NamedEnum;
import net.alex.guzhenren.gameplay.path.GuPath;
import net.minecraft.util.StringRepresentable;
import org.jetbrains.annotations.Nullable;

/**
 * 种族：人族，及各在一条道[Path]上带天赋的变异种族。
 *
 * <ul>
 *   <li>{@code BodyData} 上的封闭词表；人人生为 {@code HUMAN}，变异只能被造、不掷出；玩家只能成为 {@code DRAGONMEN}，
 *       其余十个仅 NPC；兄弟模组不得新增；{@code Race.HUMAN} 不是 {@link GuPath#HUMAN}（人道）。</li>
 *   <li>⚠ 种族可换，天赋两半须可精确撤销：{@link #TALENT_MARKS} 道痕[Path Marks]挂在种族天赋效果上而非裸池；
 *       造诣平移（{@link #TALENT_SHIFT}）而非设定。</li>
 * </ul>
 *
 * @author Alex
 * @version 1.0.0
 * @see GuPath
 * @since 1.0.0
 */

public enum Race implements NamedEnum {

    HUMAN(null),

    HAIRY_MEN(GuPath.REFINEMENT),
    EGGMEN(GuPath.SPACE),
    ROCKMEN(GuPath.EARTH),
    FEATHERMEN(GuPath.CLOUD),
    INKMEN(GuPath.INFORMATION),
    MINIMEN(GuPath.WOOD),
    MERMEN(GuPath.WATER),
    BEASTMEN(GuPath.TRANSFORMATION),
    DRAGONMEN(GuPath.ENSLAVEMENT),
    MUSHROOMMEN(GuPath.POISON),
    SNOWMEN(GuPath.ICE_SNOW);

    public static final long TALENT_MARKS = 10L;
    public static final int TALENT_SHIFT = 1;
    public static final Codec<Race> CODEC = StringRepresentable.fromEnum(Race::values);
    private final @Nullable GuPath talentPath;

    Race(@Nullable GuPath talentPath) { this.talentPath = talentPath; }

    public @Nullable GuPath getTalentPath() { return talentPath; }

    public boolean isVariant() { return talentPath != null; }

    @Override
    public String getTranslationPrefix() { return "guzhenren.enum.body.race."; }
}
