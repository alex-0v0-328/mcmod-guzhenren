package net.alex.guzhenren.item.gu.stack;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.alex.guzhenren.item.gu.tending.TendedGuItem;
import net.minecraft.network.FriendlyByteBuf;
import org.jetbrains.annotations.NotNull;

/**
 * 持续照顾型蛊[Tended Gu]单物品状态：炼化进度、饱食[Hunger]、伤势。
 * <ul>
 *   <li>⚠ 存已受伤害而非剩余生命：野生蛊的 0 即未受伤；反之每个读取方都要特判。</li>
 *   <li>存于 {@code REFINED_GU_STATE} 组件；{@code with*} 副本写成代码块：五个组件单行超 120 列。</li>
 * </ul>
 *
 * @author Alex
 * @version 1.0.0
 * @see TendedGuItem
 * @since 1.0.0
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
