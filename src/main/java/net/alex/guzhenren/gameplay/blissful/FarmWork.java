package net.alex.guzhenren.gameplay.blissful;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;
import net.alex.guzhenren.Guzhenren;
import net.alex.guzhenren.core.ModEvents;
import net.alex.guzhenren.core.Ticks;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.FarmBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.neoforged.neoforge.common.world.chunk.TicketController;
import net.neoforged.neoforge.common.world.chunk.TicketHelper;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * 地灵面板「种植」页：开垦标准田、按作物清点、收割并补种，以及田区强加载。
 * <ul>
 *   <li>标准田 = 9×9 耕地、正中一格水源（原版耕地湿润半径 4，全田都湿）；只开在福地田区（{@link BlissfulLand#farmMin()}
 *       起{@code farmChunks} 见方的区块），田区按 {@value #PITCH} 格一格划成网格、四周留 {@value #MARGIN} 格，田与田之
 *       间空 1 格过道；{@link #nextSlot} 按自北向南、自西向东取第一个能开垦的空格（{@link #isArable}）。</li>
 *   <li>能开垦：先同步加载田所在区块；9×9 每一列从地表往下找真实地面（跳过树叶、原木、可替换方块），任一列先碰到流体
 *       （水、岩浆）就算湿地不开；田的清整范围里有方块实体（箱子等）也不开，免得抹掉玩家的东西。湿地和有方块实体的田位
 *       跳过，取下一个空格。</li>
 *   <li>{@link #reclaim}：以田心的真实地面为耕地层，把 9×9 整平（上方清空至地表、至多 {@value #CLEAR_HEIGHT} 格，树一并
 *       清掉；下方向下补土至多 {@value #FILL_DEPTH} 格），再从库藏取第一种作物种子种满 80 格（不够就种够的）；之后冷却
 *       {@value #RECLAIM_COOLDOWN_TICKS} 刻。改地形先过 {@link ModEvents#canGrief}。⚠ 清整不掉落，田位上玩家搭的普通方
 *       块会被抹掉。</li>
 *   <li>作物只认 {@link #CROPS}（小麦、胡萝卜、马铃薯、甜菜根），种子也只认这四种对应的物品，其余作物种不进标准田；只在
 *       标准田里清点与收割。地狱疣要灵魂沙田，本阶段不做。</li>
 *   <li>{@link #harvest}：成熟作物按方块战利品表掉落，从中扣 1 份种子补种（掉落里没有就从库藏扣，库藏也没有就留空），
 *       其余进库藏。每块田收割后再补空格：空耕地（被踩成泥土的先重新耕好）种这块田现有最多的作物，田里一株没有就取库藏
 *       第一种种子，从库藏扣到没有为止；没种子时开出的空田、种不满的田就这样后来种满。收割与补空格合计受 {@code limit}
 *       限制。</li>
 *   <li>⚠ 强加载用 NeoForge {@link #TICKETS} 的带刻工单：原版 {@code /forceload} 的区块附近没有玩家时不随机刻，作物不
 *       长。工单以田心为主人，NeoForge 跨重启保存；失主时 {@link #release} 撤掉，认主时 {@link #retain} 重挂。读档时校
 *       验回调把无主福地（或已不是福地的维度）的工单全部丢掉，补上没装 Gu World 期间失主漏撤的工单。</li>
 * </ul>
 */

public final class FarmWork {

    private FarmWork() {}

    public static final TicketController TICKETS = new TicketController(Guzhenren.id("blissful_land_fields"),
            FarmWork::validateTickets);
    public static final int SIZE = 9;
    public static final int PITCH = SIZE + 1;
    public static final int MARGIN = 2;
    public static final int FILL_DEPTH = 8;
    public static final int CLEAR_HEIGHT = 32;
    public static final int RECLAIM_COOLDOWN_TICKS = 60 * Ticks.SECOND;
    public static final List<Block> CROPS = List.of(Blocks.WHEAT, Blocks.CARROTS, Blocks.POTATOES, Blocks.BEETROOTS);
    private static final int NO_GROUND = Integer.MIN_VALUE;

    public record Count(int mature, int growing) {}

    //region Slots [田区网格]
    public static int slotsPerSide(@NotNull BlissfulLand land) { return (land.farmChunks() * 16 - 2 * MARGIN) / PITCH; }

    public static int capacity(@NotNull BlissfulLand land) { return slotsPerSide(land) * slotsPerSide(land); }

    public static @NotNull List<BlockPos> freeSlots(@NotNull BlissfulLand land, @NotNull BlissfulLandData data) {
        int side = slotsPerSide(land);
        List<BlockPos> free = new ArrayList<>();
        for (int row = 0; row < side; row++) {
            for (int column = 0; column < side; column++) {
                int x = land.farmMin().getMinBlockX() + MARGIN + column * PITCH + SIZE / 2;
                int z = land.farmMin().getMinBlockZ() + MARGIN + row * PITCH + SIZE / 2;
                if (!occupied(data, x, z)) free.add(new BlockPos(x, 0, z));
            }
        }
        return free;
    }

    public static @Nullable BlockPos nextSlot(@NotNull ServerLevel level, @NotNull BlissfulLand land,
                                              @NotNull BlissfulLandData data) {
        for (BlockPos slot : freeSlots(land, data)) {
            if (isArable(level, slot)) return slot;
        }
        return null;
    }

    public static boolean isArable(@NotNull ServerLevel level, @NotNull BlockPos slot) {
        int half = SIZE / 2;
        for (ChunkPos chunk : chunksOf(slot)) level.getChunk(chunk.x, chunk.z);
        int ground = groundY(level, slot.getX(), slot.getZ());
        if (ground == NO_GROUND) return false;
        for (int dx = -half; dx <= half; dx++) {
            for (int dz = -half; dz <= half; dz++) {
                if (groundY(level, slot.getX() + dx, slot.getZ() + dz) == NO_GROUND) return false;
            }
        }
        BlockPos min = new BlockPos(slot.getX() - half, ground - FILL_DEPTH, slot.getZ() - half);
        BlockPos max = new BlockPos(slot.getX() + half, ground + CLEAR_HEIGHT, slot.getZ() + half);
        for (ChunkPos chunk : chunksOf(slot)) {
            for (BlockPos entity : level.getChunk(chunk.x, chunk.z).getBlockEntities().keySet()) {
                if (entity.getX() >= min.getX() && entity.getX() <= max.getX() && entity.getZ() >= min.getZ()
                        && entity.getZ() <= max.getZ() && entity.getY() >= min.getY() && entity.getY() <= max.getY()) {
                    return false;
                }
            }
        }
        return true;
    }

    private static int groundY(ServerLevel level, int x, int z) {
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos(x, level.getHeight(Heightmap.Types.WORLD_SURFACE, x,
                z) - 1, z);
        for (; pos.getY() > level.getMinBuildHeight(); pos.move(0, -1, 0)) {
            BlockState state = level.getBlockState(pos);
            if (!state.getFluidState().isEmpty()) return NO_GROUND;
            if (state.isAir() || state.is(BlockTags.LEAVES) || state.is(BlockTags.LOGS) || state.canBeReplaced()) {
                continue;
            }
            return pos.getY();
        }
        return NO_GROUND;
    }

    private static boolean occupied(BlissfulLandData data, int x, int z) {
        for (long field : data.fields()) {
            if (BlockPos.getX(field) == x && BlockPos.getZ(field) == z) return true;
        }
        return false;
    }

    public static long reclaimCooldown(@NotNull ServerLevel level, @NotNull BlissfulLandData data) {
        return Math.max(0L, data.reclaimReadyAt() - level.getGameTime());
    }
    //endregion

    //region Reclaim [开垦]
    public static @Nullable BlockPos reclaim(@NotNull ServerLevel level, @NotNull BlissfulLandData data,
                                             @NotNull BlockPos slot, @NotNull ServerPlayer owner) {
        if (!ModEvents.canGrief(level, owner)) return null;

        int half = SIZE / 2;
        if (!isArable(level, slot)) return null;
        int y = groundY(level, slot.getX(), slot.getZ());
        BlockPos center = new BlockPos(slot.getX(), y, slot.getZ());
        for (int dx = -half; dx <= half; dx++) {
            for (int dz = -half; dz <= half; dz++) level(level, center.offset(dx, 0, dz), dx == 0 && dz == 0);
        }

        ItemStack seed = data.storage().first(FarmWork::isSeed);
        if (!seed.isEmpty()) {
            Predicate<ItemStack> same = stack -> ItemStack.isSameItemSameComponents(stack, seed);
            int planted = data.storage().consume(same, SIZE * SIZE - 1);
            BlockState crop = ((BlockItem) seed.getItem()).getBlock().defaultBlockState();
            for (int dx = -half; dx <= half && planted > 0; dx++) {
                for (int dz = -half; dz <= half && planted > 0; dz++) {
                    if (dx == 0 && dz == 0) continue;
                    level.setBlock(center.offset(dx, 1, dz), crop, Block.UPDATE_ALL);
                    planted--;
                }
            }
        }
        data.addField(center);
        data.setReclaimReadyAt(level.getGameTime() + RECLAIM_COOLDOWN_TICKS);
        retain(level, data);
        return center;
    }

    private static void level(ServerLevel level, BlockPos ground, boolean water) {
        int top = Math.min(level.getHeight(Heightmap.Types.WORLD_SURFACE, ground.getX(), ground.getZ()),
                ground.getY() + CLEAR_HEIGHT);
        for (int y = top; y > ground.getY(); y--) {
            BlockPos pos = new BlockPos(ground.getX(), y, ground.getZ());
            if (level.getBlockState(pos).isAir()) continue;
            level.setBlock(pos, Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
        }
        for (int depth = 1; depth <= FILL_DEPTH; depth++) {
            BlockPos pos = ground.below(depth);
            BlockState below = level.getBlockState(pos);
            if (!below.canBeReplaced()) break;
            level.setBlock(pos, Blocks.DIRT.defaultBlockState(), Block.UPDATE_ALL);
        }
        BlockState surface = water ? Blocks.WATER.defaultBlockState()
                : Blocks.FARMLAND.defaultBlockState().setValue(FarmBlock.MOISTURE, FarmBlock.MAX_MOISTURE);
        level.setBlock(ground, surface, Block.UPDATE_ALL);
    }

    public static boolean isSeed(@NotNull ItemStack stack) {
        return stack.getItem() instanceof BlockItem item && CROPS.contains(item.getBlock());
    }
    //endregion

    //region Crops [作物]
    public static boolean isCrop(@NotNull BlockState state) { return CROPS.contains(state.getBlock()); }

    public static boolean isMature(@NotNull BlockState state) {
        return state.getBlock() instanceof CropBlock crop && crop.isMaxAge(state);
    }

    public static @NotNull Map<Block, Count> census(@NotNull ServerLevel level, @NotNull BlissfulLandData data) {
        Map<Block, int[]> counts = new LinkedHashMap<>();
        for (BlockPos pos : cropCells(level, data)) {
            BlockState state = level.getBlockState(pos);
            if (!isCrop(state)) continue;
            int[] count = counts.computeIfAbsent(state.getBlock(), block -> new int[2]);
            count[isMature(state) ? 0 : 1]++;
        }
        Map<Block, Count> census = new LinkedHashMap<>();
        counts.forEach((block, count) -> census.put(block, new Count(count[0], count[1])));
        return census;
    }

    public static int harvest(@NotNull ServerLevel level, @NotNull BlissfulLandData data, int limit) {
        int handled = 0;
        for (long field : data.fields()) {
            BlockPos center = BlockPos.of(field);
            if (!level.isLoaded(center)) continue;
            List<BlockPos> cells = cellsOf(center);
            for (BlockPos pos : cells) {
                if (handled >= limit) return handled;
                BlockState state = level.getBlockState(pos);
                if (!isCrop(state) || !isMature(state)) continue;

                reap(level, data, pos, state);
                handled++;
            }
            handled += sow(level, data, cells, limit - handled);
        }
        return handled;
    }

    private static void reap(ServerLevel level, BlissfulLandData data, BlockPos pos, BlockState state) {
        Item seed = state.getBlock().asItem();
        boolean replanted = false;
        for (ItemStack drop : Block.getDrops(state, level, pos, null)) {
            if (!replanted && drop.is(seed)) {
                drop.shrink(1);
                replanted = true;
            }
            if (!drop.isEmpty()) data.storage().insert(drop);
        }
        if (!replanted) replanted = data.storage().consume(stack -> stack.is(seed), 1) == 1;
        BlockState next = replanted ? state.getBlock().defaultBlockState() : Blocks.AIR.defaultBlockState();
        level.setBlock(pos, next, Block.UPDATE_ALL);
    }

    private static int sow(ServerLevel level, BlissfulLandData data, List<BlockPos> cells, int limit) {
        Block crop = cropOf(level, cells);
        if (crop == null) {
            ItemStack seed = data.storage().first(FarmWork::isSeed);
            if (seed.isEmpty()) return 0;
            crop = ((BlockItem) seed.getItem()).getBlock();
        }
        Item seed = crop.asItem();
        int sown = 0;
        for (BlockPos pos : cells) {
            if (sown >= limit) break;
            BlockState ground = level.getBlockState(pos.below());
            if (!level.getBlockState(pos).isAir() || !ground.is(Blocks.FARMLAND) && !ground.is(Blocks.DIRT)) continue;
            if (data.storage().consume(stack -> stack.is(seed), 1) == 0) break;

            if (ground.is(Blocks.DIRT)) {
                level.setBlock(pos.below(), Blocks.FARMLAND.defaultBlockState()
                        .setValue(FarmBlock.MOISTURE, FarmBlock.MAX_MOISTURE), Block.UPDATE_ALL);
            }
            level.setBlock(pos, crop.defaultBlockState(), Block.UPDATE_ALL);
            sown++;
        }
        return sown;
    }

    private static @Nullable Block cropOf(ServerLevel level, List<BlockPos> cells) {
        Map<Block, Integer> counts = new LinkedHashMap<>();
        for (BlockPos pos : cells) {
            BlockState state = level.getBlockState(pos);
            if (isCrop(state)) counts.merge(state.getBlock(), 1, Integer::sum);
        }
        return counts.entrySet().stream().max(Map.Entry.comparingByValue()).map(Map.Entry::getKey).orElse(null);
    }

    private static List<BlockPos> cropCells(ServerLevel level, BlissfulLandData data) {
        List<BlockPos> cells = new ArrayList<>();
        for (long field : data.fields()) {
            BlockPos center = BlockPos.of(field);
            if (level.isLoaded(center)) cells.addAll(cellsOf(center));
        }
        return cells;
    }

    private static List<BlockPos> cellsOf(BlockPos center) {
        List<BlockPos> cells = new ArrayList<>(SIZE * SIZE - 1);
        int half = SIZE / 2;
        for (int dx = -half; dx <= half; dx++) {
            for (int dz = -half; dz <= half; dz++) {
                if (dx != 0 || dz != 0) cells.add(center.offset(dx, 1, dz));
            }
        }
        return cells;
    }
    //endregion

    //region Tickets [强加载]
    public static void retain(@NotNull ServerLevel level, @NotNull BlissfulLandData data) {
        setForced(level, data, true);
    }

    public static void release(@NotNull ServerLevel level, @NotNull BlissfulLandData data) {
        setForced(level, data, false);
    }

    private static void validateTickets(ServerLevel level, TicketHelper tickets) {
        BlissfulLand land = BlissfulLands.landOf(level);
        if (land != null && BlissfulLandData.get(level.getServer(), land).hasOwner()) return;
        for (BlockPos owner : List.copyOf(tickets.getBlockTickets().keySet())) tickets.removeAllTickets(owner);
    }

    private static void setForced(ServerLevel level, BlissfulLandData data, boolean add) {
        for (long field : data.fields()) {
            BlockPos center = BlockPos.of(field);
            for (ChunkPos chunk : chunksOf(center)) TICKETS.forceChunk(level, center, chunk.x, chunk.z, add, true);
        }
    }

    private static List<ChunkPos> chunksOf(BlockPos center) {
        int half = SIZE / 2;
        List<ChunkPos> chunks = new ArrayList<>(4);
        for (int x : new int[] {center.getX() - half, center.getX() + half}) {
            for (int z : new int[] {center.getZ() - half, center.getZ() + half}) {
                ChunkPos chunk = new ChunkPos(x >> 4, z >> 4);
                if (!chunks.contains(chunk)) chunks.add(chunk);
            }
        }
        return chunks;
    }
    //endregion
}
