package net.alex.guzhenren.gameplay.immortalaperture.spring;

import it.unimi.dsi.fastutil.longs.LongArrayList;
import it.unimi.dsi.fastutil.longs.LongList;
import net.alex.guzhenren.gameplay.immortalaperture.state.BlissfulLand;
import net.alex.guzhenren.gameplay.immortalaperture.state.BlissfulLandData;
import net.alex.guzhenren.gameplay.immortalaperture.state.BlissfulLandLookup;
import net.alex.guzhenren.gameplay.immortalaperture.state.Territory;
import net.alex.guzhenren.gameplay.immortalaperture.storage.BlissfulStorage;
import net.alex.guzhenren.registry.block.ModBlocks;
import net.alex.guzhenren.registry.item.ModItems;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.chunk.LevelChunkSection;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * 福地「引泉」：地灵把福地里每眼元泉[Spirit Spring]产的元石[Primeval Stone]收进库藏。
 * <ul>
 *   <li>登记：区块加载时（{@link #onChunkLoad}，生成、地图探明、走到都算）整块重扫元泉源方块并登记泉位，运行时放下的
 *       源方块经 {@link #onPlaced} 登记；没生成过的泉不在名单里。名单在 {@link SpringState}，失主不清。</li>
 *   <li>{@link #host} 是托管任务，福地有主就每 60 秒跑一轮（主人不在线也跑，停服期间不补发）：名单里每眼泉折算
 *       {@link SpringRules#STONES_PER_SPRING_PER_ROUND} 颗元石直入库藏，不要求泉所在区块加载、不要求玩家在场、不设上限。</li>
 *   <li>结算时先核对：泉所在区块已被割弃出疆域的注销；区块已加载而那里不再是元泉源（被舀掉、被换掉）的注销，放回源方
 *       块后下次区块加载或放置时再登记；区块没加载的按名单照算，没人够得着它。注销的当轮不付。</li>
 *   <li>{@link #collects}：福地有主时福地里的元泉方块不再弹掉落物（产出全由地灵收，免得站在泉边收益翻倍）；无主与别的世
 *       界照旧。元泉方块的计划刻用它判断。</li>
 * </ul>
 */

public final class SpringWork {

    private SpringWork() {}

    public static int host(@NotNull ServerLevel level, @NotNull BlissfulLandData data, @Nullable ServerPlayer owner) {
        return settle(level, data);
    }

    public static int settle(@NotNull ServerLevel level, @NotNull BlissfulLandData data) {
        SpringState springs = SpringState.of(data);
        Territory territory = data.territory();
        int paid = 0;
        for (long packed : springs.positions()) {
            BlockPos pos = BlockPos.of(packed);
            int chunkX = pos.getX() >> 4;
            int chunkZ = pos.getZ() >> 4;
            if (!territory.contains(chunkX, chunkZ)) {
                springs.remove(pos);
            } else if (level.getChunkSource().getChunkNow(chunkX, chunkZ) instanceof LevelChunk chunk
                    && !isSource(chunk.getBlockState(pos))) {
                springs.remove(pos);
            } else {
                paid++;
            }
        }
        if (paid > 0) {
            long stones = Math.min(SpringRules.stonesPerRound(paid), Integer.MAX_VALUE);
            BlissfulStorage.of(data).insert(new ItemStack(ModItems.PRIMEVAL_STONE.get(), (int) stones));
        }
        return paid;
    }

    public static void onChunkLoad(@NotNull BlissfulLandData data, @NotNull LevelChunk chunk) {
        ChunkPos pos = chunk.getPos();
        SpringState springs = SpringState.of(data);
        springs.replaceChunk(pos, data.territory().contains(pos.x, pos.z) ? scan(chunk) : new LongArrayList());
    }

    public static void onPlaced(@NotNull ServerLevel level, @NotNull BlockPos pos) {
        BlissfulLand land = BlissfulLandLookup.landOf(level);
        if (land == null || !isSource(level.getBlockState(pos))) return;

        BlissfulLandData data = BlissfulLandData.get(level.getServer(), land);
        if (data.territory().contains(pos.getX() >> 4, pos.getZ() >> 4)) SpringState.of(data).add(pos);
    }

    public static boolean collects(@NotNull Level level) {
        if (!(level instanceof ServerLevel server)) return false;

        BlissfulLand land = BlissfulLandLookup.landOf(level);
        return land != null && BlissfulLandData.get(server.getServer(), land).hasOwner();
    }

    public static boolean isSource(@NotNull BlockState state) {
        return state.is(ModBlocks.SPIRIT_SPRING) && state.getFluidState().isSource();
    }

    static @NotNull LongList scan(@NotNull LevelChunk chunk) {
        LongList found = new LongArrayList();
        ChunkPos chunkPos = chunk.getPos();
        LevelChunkSection[] sections = chunk.getSections();
        BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();
        for (int index = 0; index < sections.length; index++) {
            LevelChunkSection section = sections[index];
            if (section.hasOnlyAir() || !section.maybeHas(state -> state.is(ModBlocks.SPIRIT_SPRING))) continue;

            int baseY = chunk.getSectionYFromSectionIndex(index) << 4;
            for (int y = 0; y < 16; y++) {
                for (int z = 0; z < 16; z++) {
                    for (int x = 0; x < 16; x++) {
                        if (!isSource(section.getBlockState(x, y, z))) continue;

                        found.add(cursor.set(chunkPos.getBlockX(x), baseY + y, chunkPos.getBlockZ(z)).asLong());
                    }
                }
            }
        }
        return found;
    }
}
