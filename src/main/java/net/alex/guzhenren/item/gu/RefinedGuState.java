package net.alex.guzhenren.item.gu;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

/**
 * The per-stack state a tended Gu [需照顾] carries: how far refined, how well fed, how badly hurt.
 *
 * <p>An immutable record stored on the {@code REFINED_GU_STATE} data component. Its five fields fit
 * the composite stream codec. The compact constructor clamps every field to {@code >= 0}.
 *
 * <p>⚠ Stores damage taken rather than health remaining, so a wild Gu's zero already reads as
 * undamaged. Turning that around would need a special case in every reader.
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
    public static final StreamCodec<ByteBuf, RefinedGuState> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.BOOL, RefinedGuState::refined,
            ByteBufCodecs.VAR_INT, RefinedGuState::refineProgress,
            ByteBufCodecs.VAR_INT, RefinedGuState::investedEssence,
            ByteBufCodecs.VAR_INT, RefinedGuState::hunger,
            ByteBufCodecs.VAR_INT, RefinedGuState::damageTaken,
            RefinedGuState::new);

    public RefinedGuState {
        refineProgress = Math.max(0, refineProgress);
        investedEssence = Math.max(0, investedEssence);
        hunger = Math.max(0, hunger);
        damageTaken = Math.max(0, damageTaken);
    }

    //region the with* copies -- five components run past 120, so these are blocks
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
