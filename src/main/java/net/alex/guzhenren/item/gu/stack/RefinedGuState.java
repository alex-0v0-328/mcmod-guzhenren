package net.alex.guzhenren.item.gu.stack;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.alex.guzhenren.item.gu.tending.TendedGuItem;
import net.minecraft.network.FriendlyByteBuf;
import org.jetbrains.annotations.NotNull;

/**
 * 持续照顾型蛊[Tended Gu]的单物品栈状态记录：保存炼化进度、已投入真元与饱食[Hunger]。
 * <ul>
 *   <li>本记录挂载于 {@code REFINED_GU_STATE} 数据组件；伤势另存 {@link GuCondition}，缺省即正常。</li>
 *   <li>四个字段的 {@code with*} 副本方法拆写成代码块，以便将单行长度控制在 120 列以内。</li>
 * </ul>
 */

public record RefinedGuState(boolean refined, int refineProgress, int investedEssence, int hunger) {

    public static final RefinedGuState WILD = new RefinedGuState(false, 0, 0, 0);
    public static final Codec<RefinedGuState> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.BOOL.optionalFieldOf("refined", false).forGetter(RefinedGuState::refined),
            Codec.INT.optionalFieldOf("refine_progress", 0).forGetter(RefinedGuState::refineProgress),
            Codec.INT.optionalFieldOf("invested_essence", 0).forGetter(RefinedGuState::investedEssence),
            Codec.INT.optionalFieldOf("hunger", 0).forGetter(RefinedGuState::hunger)
    ).apply(instance, RefinedGuState::new));

    public static @NotNull RefinedGuState decode(@NotNull FriendlyByteBuf buf) {
        return new RefinedGuState(buf.readBoolean(), buf.readVarInt(), buf.readVarInt(), buf.readVarInt());
    }

    public static void encode(@NotNull FriendlyByteBuf buf, @NotNull RefinedGuState value) {
        buf.writeBoolean(value.refined());
        buf.writeVarInt(value.refineProgress());
        buf.writeVarInt(value.investedEssence());
        buf.writeVarInt(value.hunger());
    }

    public RefinedGuState {
        refineProgress = Math.max(0, refineProgress);
        investedEssence = Math.max(0, investedEssence);
        hunger = Math.max(0, hunger);
    }

    //region the with* copies [with 副本]
    public RefinedGuState withRefine(int value) {
        return new RefinedGuState(refined, value, investedEssence, hunger);
    }

    public RefinedGuState withInvested(int value) {
        return new RefinedGuState(refined, refineProgress, value, hunger);
    }

    public RefinedGuState withHunger(int value) {
        return new RefinedGuState(refined, refineProgress, investedEssence, value);
    }
    //endregion
}
