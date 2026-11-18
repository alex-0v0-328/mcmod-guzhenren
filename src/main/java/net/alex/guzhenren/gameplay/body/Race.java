package net.alex.guzhenren.gameplay.body;

import com.mojang.serialization.Codec;
import net.alex.guzhenren.core.NamedEnum;
import net.alex.guzhenren.gameplay.path.GuPath;
import net.minecraft.util.StringRepresentable;
import org.jetbrains.annotations.Nullable;

/**
 * 肉身种族枚举：包含人族（{@link #HUMAN}）以及各自在一条流派上拥有天赋的十一种异人种族。
 * <ul>
 *   <li>存于 {@link BodyData#race} 的封闭词汇表；角色出生默认为 {@code HUMAN}，异人种族仅能通过转化获得而不会随机掷出
 *       （玩家仅能转化为龙人 {@link #DRAGONMEN}，其余十种仅供 NPC 使用）；注意 {@link #HUMAN} 指种族，而非人道
 *       （{@link GuPath#HUMAN}）。</li>
 *   <li>⚠ 种族允许切换，因此天赋的两项加成必须支持对称撤销：道痕[Path Marks]加成（{@link #TALENT_MARKS}）以永久流派效果
 *       挂载而非直接写入裸道痕池，流派造诣则通过相对平移（{@link #TALENT_SHIFT}）增减而非直接覆写。</li>
 * </ul>
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
