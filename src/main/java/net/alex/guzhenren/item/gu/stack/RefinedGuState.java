package net.alex.guzhenren.item.gu.stack;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.alex.guzhenren.item.gu.tending.TendedGuItem;
import net.minecraft.network.FriendlyByteBuf;
import org.jetbrains.annotations.NotNull;

/**
 * 持续照顾型蛊[Tended Gu]的单物品栈状态记录：保存炼化进度、已投入真元、饱食[Hunger]与已受伤害。
 * <ul>
 *   <li>⚠ 伤势字段记录已受伤害而非剩余生命值：野生蛊默认值为 0 即表示未受伤；若改为存储剩余生命，则所有读取方都必须额
 *       外特判未初始化状态。</li>
 *   <li>本记录挂载于 {@code REFINED_GU_STATE} 数据组件；五个字段的 {@code with*} 副本方法拆写成代码块，以便将单行长度控
 *       制在 120 列以内。</li>
 * </ul>
 */

public record RefinedGuState(boolean refined, int refineProgress, int investedEssence, int hunger,
                             int damageTaken) {

    public static final RefinedGuState WILD = new RefinedGuState(false, 0, 0, 0, 0);
    public static final Codec<RefinedGuState> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.BOOL.optionalFieldOf("refined", false).forGetter(RefinedGuState::refined),
            Codec.INT.optionalFieldOf("refine_progress", 0).forGetter(RefinedGuState::refineProgress),
            Codec.INT.optionalFieldOf("invested_essence", 0).forGetter(RefinedGuState::investedEssence),
            Codec.INT.optionalFieldOf("hunger", 0).forGetter(RefinedGuState::hunger),
            Codec.INT.optionalFieldOf("damage_taken", 0).forGetter(RefinedGuState::damageTaken)
    ).apply(instance, RefinedGuState::new));

    public static @NotNull RefinedGuState decode(@NotNull FriendlyByteBuf buf) {
        return new RefinedGuState(buf.readBoolean(), buf.readVarInt(), buf.readVarInt(), buf.readVarInt(),
                buf.readVarInt());
    }

    public static void encode(@NotNull FriendlyByteBuf buf, @NotNull RefinedGuState value) {
        buf.writeBoolean(value.refined());
        buf.writeVarInt(value.refineProgress());
        buf.writeVarInt(value.investedEssence());
        buf.writeVarInt(value.hunger());
        buf.writeVarInt(value.damageTaken());
    }

    public RefinedGuState {
        refineProgress = Math.max(0, refineProgress);
        investedEssence = Math.max(0, investedEssence);
        hunger = Math.max(0, hunger);
        damageTaken = Math.max(0, damageTaken);
    }

    //region the with* copies [with 副本]
    public RefinedGuState withRefine(int value) {
        return new RefinedGuState(refined, value, investedEssence, hunger, damageTaken);
    }

    public RefinedGuState withInvested(int value) {
        return new RefinedGuState(refined, refineProgress, value, hunger, damageTaken);
    }

    public RefinedGuState withHunger(int value) {
        return new RefinedGuState(refined, refineProgress, investedEssence, value, damageTaken);
    }

    public RefinedGuState withDamageTaken(int value) {
        return new RefinedGuState(refined, refineProgress, investedEssence, hunger, value);
    }
    //endregion
}
