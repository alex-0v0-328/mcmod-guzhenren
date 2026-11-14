package net.alex.guzhenren.item.gu.mortal.path.wood;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.util.Mth;
import org.jetbrains.annotations.NotNull;

/**
 * 九叶生机草[Nine Leaf Vitality Grass]叶片：上次写入的叶数与结算时刻（主世界游戏时间，各维度共用）。
 * <ul>
 *   <li>存于 {@code LEAF_GROWTH} 组件，不逐 tick 更新；{@link #settle} 推算任一时刻叶数，每 {@code regrowTicks}
 *       一片，至多 {@link #MAX_LEAVES}；箱中、地上、他人包里照长，两端同戳同数。</li>
 *   <li>⚠ 满叶结算把戳停在当下，摘首片后从摘时再生；未满保留下一片进度，摘、长都不丢；戳超前时钟（跨存档搬动）
 *       则重新等待，不冻结。</li>
 *   <li>{@link #getPickedStage} 给模型三态：≥ 7 片满，≥ 1 片半，0 片空。</li>
 * </ul>
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

    public static @NotNull LeafGrowth decode(@NotNull FriendlyByteBuf buf) {
        return new LeafGrowth(buf.readVarInt(), buf.readVarLong());
    }

    public static void encode(@NotNull FriendlyByteBuf buf, @NotNull LeafGrowth value) {
        buf.writeVarInt(value.leaves());
        buf.writeVarLong(value.settledAt());
    }

    public LeafGrowth {
        leaves = Mth.clamp(leaves, 0, MAX_LEAVES);
    }

    public LeafGrowth settle(long now, int regrowTicks) {
        if (leaves >= MAX_LEAVES || now < settledAt) return new LeafGrowth(leaves, now);

        long grown = (now - settledAt) / regrowTicks;
        if (leaves + grown >= MAX_LEAVES) return new LeafGrowth(MAX_LEAVES, now);
        return new LeafGrowth(leaves + (int) grown, settledAt + grown * regrowTicks);
    }

    public int getTicksToNextLeaf(long now, int regrowTicks) {
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

    public static float getPickedStage(int leaves) {
        if (leaves >= FULL_LOOK_FROM) return 0.0F;
        return leaves > 0 ? PICKED_HALF : PICKED_BARE;
    }
}
