package net.alex.guzhenren.gameplay.secretrealm;

import it.unimi.dsi.fastutil.longs.Long2IntLinkedOpenHashMap;
import it.unimi.dsi.fastutil.longs.Long2IntMap;
import it.unimi.dsi.fastutil.longs.LongArrayList;
import net.alex.guzhenren.core.ModSavedData;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.ByteArrayTag;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.LongArrayTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.saveddata.SavedData;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * 单维度内由荡魂山[Soulquake Mountain]长出、尚未敲碎的胆石[Guts Stone]格子与每格块数，持久化于该维度的
 * {@code guzhenren_guts_stones.dat}。
 * <ul>
 *   <li>每格记坐标（{@link BlockPos#asLong}）与块数 1–{@value #MAX_STONES}，{@link #total} 为全部块数之和，
 *       {@link SoulquakeMountain#GUTS_STONE_CAP} 按它算；胆石方块块数变化或被换掉时由 {@code GutsStoneBlock#onRemove} 调
 *       {@link #set} 或 {@link #remove}。</li>
 *   <li>{@link #STONES} 是胆石方块的块数属性；定义在这里而非方块类，因为长胆石要写它，而 gameplay 包不能反向依赖
 *       block 包。</li>
 *   <li>⚠ 性能：至多 128 格，存为一个 {@link LongArrayTag} 加一个 {@link ByteArrayTag}；{@link #pickStackable} 遍历全部格
 *       子，只在生物死亡长胆石时调用。</li>
 * </ul>
 */

public final class GutsStones extends ModSavedData {

    public static final String NAME = "guzhenren_guts_stones";
    public static final int MAX_STONES = 3;
    public static final IntegerProperty STONES = IntegerProperty.create("stones", 1, MAX_STONES);
    private static final String POSITIONS = "positions";
    private static final String COUNTS = "stones";
    private static final SavedData.Factory<GutsStones> FACTORY =
            new SavedData.Factory<>(GutsStones::new, (tag, registries) -> load(tag));
    private final Long2IntLinkedOpenHashMap cells = new Long2IntLinkedOpenHashMap();
    private int total;

    public static @NotNull GutsStones get(@NotNull ServerLevel level) { return ModSavedData.get(level, NAME, FACTORY); }

    public int size() { return cells.size(); }

    public int total() { return total; }

    public boolean contains(@NotNull BlockPos pos) { return cells.containsKey(pos.asLong()); }

    public int count(@NotNull BlockPos pos) { return cells.get(pos.asLong()); }

    public void add(@NotNull BlockPos pos) { set(pos, 1); }

    public void set(@NotNull BlockPos pos, int stones) {
        int clamped = Math.clamp(stones, 1, MAX_STONES);
        int before = cells.put(pos.asLong(), clamped);
        if (before == clamped) return;

        total += clamped - before;
        setDirty();
    }

    public void remove(@NotNull BlockPos pos) {
        if (!cells.containsKey(pos.asLong())) return;

        total -= cells.remove(pos.asLong());
        setDirty();
    }

    public @Nullable BlockPos pickStackable(@NotNull RandomSource random) {
        LongArrayList open = new LongArrayList();
        for (Long2IntMap.Entry cell : cells.long2IntEntrySet()) {
            if (cell.getIntValue() < MAX_STONES) open.add(cell.getLongKey());
        }
        return open.isEmpty() ? null : BlockPos.of(open.getLong(random.nextInt(open.size())));
    }

    @Override
    protected void write(@NotNull CompoundTag tag, @NotNull StackIo stacks) {
        long[] positions = new long[cells.size()];
        byte[] counts = new byte[cells.size()];
        int i = 0;
        for (Long2IntMap.Entry cell : cells.long2IntEntrySet()) {
            positions[i] = cell.getLongKey();
            counts[i++] = (byte) cell.getIntValue();
        }
        tag.put(POSITIONS, new LongArrayTag(positions));
        tag.put(COUNTS, new ByteArrayTag(counts));
    }

    private static GutsStones load(CompoundTag tag) {
        GutsStones stones = new GutsStones();
        long[] positions = tag.getLongArray(POSITIONS);
        byte[] counts = tag.getByteArray(COUNTS);
        for (int i = 0; i < positions.length; i++) {
            stones.set(BlockPos.of(positions[i]), counts[i]);
        }
        stones.setDirty(false);
        return stones;
    }
}
