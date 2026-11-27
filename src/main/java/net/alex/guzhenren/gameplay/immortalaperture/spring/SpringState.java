package net.alex.guzhenren.gameplay.immortalaperture.spring;

import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.longs.LongArrayList;
import it.unimi.dsi.fastutil.longs.LongList;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import net.alex.guzhenren.core.ModSavedData.StackIo;
import net.alex.guzhenren.gameplay.immortalaperture.state.BlissfulLandData;
import net.alex.guzhenren.gameplay.immortalaperture.state.LandSavePart;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.LongArrayTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.level.ChunkPos;
import org.jetbrains.annotations.NotNull;

/**
 * 福地登记的元泉[Spirit Spring]泉位：地灵每轮托管按这份名单结算，不要求泉所在的区块加载。
 * <ul>
 *   <li>泉位是元泉源方块的坐标，按区块分桶：区块加载时整块重扫（{@link #replaceChunk}）校正本区块的登记，一个区块里的
 *       泉位互不影响别的区块。</li>
 *   <li>{@link #positions} 给结算一份快照，结算边遍历边注销（{@link #remove}）不会改到正在遍历的集合。</li>
 *   <li>本类是存档段 {@link #PART}（子项 {@code springs}，坐标的长整数组），任何改动经回调通知所属存档置脏；失主不清名单，
 *       留给下一任。</li>
 * </ul>
 */

public final class SpringState implements LandSavePart.State {

    public static final LandSavePart<SpringState> PART = new LandSavePart<>("springs", SpringState::new);

    private final Runnable changed;
    private final Long2ObjectOpenHashMap<LongOpenHashSet> byChunk = new Long2ObjectOpenHashMap<>();
    private int count;

    public SpringState(@NotNull Runnable changed) { this.changed = changed; }

    public static @NotNull SpringState of(@NotNull BlissfulLandData data) { return data.part(PART); }

    public int count() { return count; }

    public boolean contains(@NotNull BlockPos pos) {
        LongOpenHashSet chunk = byChunk.get(chunkKey(pos));
        return chunk != null && chunk.contains(pos.asLong());
    }

    public @NotNull LongList positions() {
        LongList all = new LongArrayList(count);
        for (LongOpenHashSet chunk : byChunk.values()) all.addAll(chunk);
        return all;
    }

    public boolean add(@NotNull BlockPos pos) {
        boolean added = byChunk.computeIfAbsent(chunkKey(pos), key -> new LongOpenHashSet()).add(pos.asLong());
        if (added) {
            count++;
            changed.run();
        }
        return added;
    }

    public boolean remove(@NotNull BlockPos pos) {
        long key = chunkKey(pos);
        LongOpenHashSet chunk = byChunk.get(key);
        if (chunk == null || !chunk.remove(pos.asLong())) return false;

        if (chunk.isEmpty()) byChunk.remove(key);
        count--;
        changed.run();
        return true;
    }

    public void replaceChunk(@NotNull ChunkPos chunk, @NotNull LongList found) {
        long key = chunk.toLong();
        LongOpenHashSet next = new LongOpenHashSet(found);
        LongOpenHashSet before = byChunk.get(key);
        if (before == null ? next.isEmpty() : before.equals(next)) return;

        count += next.size() - (before == null ? 0 : before.size());
        if (next.isEmpty()) {
            byChunk.remove(key);
        } else {
            byChunk.put(key, next);
        }
        changed.run();
    }

    private static long chunkKey(BlockPos pos) { return ChunkPos.asLong(pos.getX() >> 4, pos.getZ() >> 4); }

    @Override
    public void save(@NotNull CompoundTag tag, @NotNull String key, @NotNull StackIo stacks) {
        tag.put(key, new LongArrayTag(positions().toLongArray()));
    }

    @Override
    public void load(@NotNull CompoundTag tag, @NotNull String key, @NotNull StackIo stacks) {
        byChunk.clear();
        count = 0;
        if (!tag.contains(key, Tag.TAG_LONG_ARRAY)) return;

        for (long packed : tag.getLongArray(key)) {
            if (byChunk.computeIfAbsent(chunkKey(BlockPos.of(packed)), chunk -> new LongOpenHashSet()).add(packed)) {
                count++;
            }
        }
    }
}
