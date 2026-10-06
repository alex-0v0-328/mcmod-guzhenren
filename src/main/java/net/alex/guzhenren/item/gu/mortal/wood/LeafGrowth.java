package net.alex.guzhenren.item.gu.mortal.wood;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

/**
 * The leaves on one Nine Leaf Vitality Grass [九叶生机草]: how many were left when the stack was last written,
 * and the game time that count was settled at.
 *
 * <p>Stored on the {@code LEAF_GROWTH} data component and never ticked. {@link #settle} derives the count at
 * any later game time -- one leaf per {@code regrowTicks}, {@link #MAX_LEAVES} at most -- so a grass in a chest,
 * on the ground or in another player's bag keeps growing with nobody watching, and server and client derive the
 * same count from the same stamp. Game time is one clock for every dimension (the others read the overworld's).
 *
 * <p>⚠ A full grass parks its stamp at the moment it is settled: the regrowth of the first picked leaf starts at
 * the pick, not at the moment the grass last filled up. A partial count keeps the time already grown toward the
 * next leaf, so neither a pick nor a grown leaf throws that progress away. A stamp ahead of the clock (a stack
 * moved between saves) restarts the wait instead of freezing it.
 *
 * <p>{@link #pickedStage} is the item model's three looks: full at seven leaves and up, half from one, empty at
 * none.
 *
 * @author Alex
 * @version 1.0.0
 * @see NineLeafVitalityGrassItem
 * @since 1.0.0
 */

public record LeafGrowth(int leaves, long settledAt) {

    public static final int MAX_LEAVES = 9;
    public static final LeafGrowth FULL = new LeafGrowth(MAX_LEAVES, 0L);
    public static final float PICKED_HALF = 0.5F;
    public static final float PICKED_BARE = 1.0F;
    private static final int FULL_LOOK_FROM = 7;
    public static final Codec<LeafGrowth> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.INT.optionalFieldOf("leaves", MAX_LEAVES).forGetter(LeafGrowth::leaves),
            Codec.LONG.optionalFieldOf("settled_at", 0L).forGetter(LeafGrowth::settledAt)
    ).apply(instance, LeafGrowth::new));
    public static final StreamCodec<ByteBuf, LeafGrowth> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, LeafGrowth::leaves,
            ByteBufCodecs.VAR_LONG, LeafGrowth::settledAt,
            LeafGrowth::new);

    public LeafGrowth {
        leaves = Math.clamp(leaves, 0, MAX_LEAVES);
    }

    public LeafGrowth settle(long now, int regrowTicks) {
        if (leaves >= MAX_LEAVES || now < settledAt) return new LeafGrowth(leaves, now);

        long grown = (now - settledAt) / regrowTicks;
        if (leaves + grown >= MAX_LEAVES) return new LeafGrowth(MAX_LEAVES, now);
        return new LeafGrowth(leaves + (int) grown, settledAt + grown * regrowTicks);
    }

    public int ticksToNextLeaf(long now, int regrowTicks) {
        LeafGrowth settled = settle(now, regrowTicks);
        return settled.leaves >= MAX_LEAVES ? 0 : (int) (regrowTicks - (now - settled.settledAt));
    }

    public LeafGrowth pick(long now, int regrowTicks) {
        LeafGrowth settled = settle(now, regrowTicks);
        return settled.leaves <= 0 ? settled : new LeafGrowth(settled.leaves - 1, settled.settledAt);
    }

    public LeafGrowth grow(long now, int regrowTicks) {
        LeafGrowth settled = settle(now, regrowTicks);
        return settled.leaves + 1 >= MAX_LEAVES
                ? new LeafGrowth(MAX_LEAVES, now)
                : new LeafGrowth(settled.leaves + 1, settled.settledAt);
    }

    public static float pickedStage(int leaves) {
        if (leaves >= FULL_LOOK_FROM) return 0.0F;
        return leaves > 0 ? PICKED_HALF : PICKED_BARE;
    }
}
