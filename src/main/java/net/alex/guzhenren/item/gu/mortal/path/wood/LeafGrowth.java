package net.alex.guzhenren.item.gu.mortal.path.wood;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.util.Mth;
import org.jetbrains.annotations.NotNull;

/**
 * 九叶生机草[Nine Leaf Vitality Grass]（{@link NineLeafVitalityGrassItem}）的叶片生长状态组件数据：记录上次写入时的叶片
 * 数与结算时刻（统一取主世界游戏时间，跨维度通用）。
 * <ul>
 *   <li>保存在 {@code LEAF_GROWTH} 数据组件中，不在每 tick 写回物品栈；{@link #settle} 根据当前时刻与
 *       {@code regrowTicks} 按需惰性推算已长出的叶片数（上限 {@link #MAX_LEAVES}），无论物品放在箱子、地面还是他人背包
 *       中均照常生长，双端读取同一时间戳即可算出一致的叶片数。</li>
 *   <li>⚠ 满叶时 {@link #settle} 会将结算时间戳推进至当前时刻，使摘下首片叶后从采摘瞬间重新计时；未满叶时采摘
 *       （{@link #pick}）或催熟（{@link #grow}）均保留下一片叶的已累积进度；若存档迁移导致记录的时间戳超前于当前世界时
 *       间，自动重置起点重新等待而不会永久卡死。</li>
 *   <li>{@link #getPickedStage} 提供物品模型三档外观阈值：7～9 片为满叶（{@code 0.0F}），1～6 片为半叶
 *       （{@link #PICKED_HALF}），0 片为光杆（{@link #PICKED_BARE}）。</li>
 * </ul>
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
