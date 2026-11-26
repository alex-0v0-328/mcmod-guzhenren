package net.alex.guzhenren.gameplay.blissful.farm;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;
import net.alex.guzhenren.Guzhenren;
import net.alex.guzhenren.core.ModEvents;
import net.alex.guzhenren.core.Ticks;
import net.alex.guzhenren.gameplay.blissful.land.BlissfulLand;
import net.alex.guzhenren.gameplay.blissful.land.BlissfulLandData;
import net.alex.guzhenren.gameplay.blissful.land.Lands;
import net.alex.guzhenren.gameplay.blissful.map.MapScanner;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
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
 * 地灵面板的标准田：开垦、拆田、按作物清点、收割并补种，以及田的强加载。选址与规划队列在 {@link FieldPlans}。
 * <ul>
 *   <li>{@link #reclaim}：先同步加载田所在区块；每列从地表往下找真实地面（跳过 {@link MapScanner#isPassable}），任一列先
 *       碰到流体就不开（选址已查过，这里只防开垦前地形变了）；以 {@link FieldPlot#layer}（列数最多的地面层）为耕地层，
 *       把整块田整平（上方清空至地表、至多 {@value #CLEAR_HEIGHT} 格，树一并清掉；下方向下补土至多
 *       {@value #FILL_DEPTH} 格），按 {@link FieldPlot} 铺土径、水与耕地，再按主人选的开垦作物（{@link FarmState#crop()}，
 *       自动 = 库藏第一种种子）从库藏扣种子种满耕地（不够就种够的，没有就开空田，留给之后的收割补种）；之后冷却
 *       {@value #RECLAIM_COOLDOWN_TICKS} 刻。改地形先过 {@link ModEvents#canGrief}。</li>
 *   <li>⚠ {@link #reclaim} 清整不掉落任何方块，田上玩家搭的普通方块会被直接抹掉；有方块实体的区块选址时已拒。</li>
 *   <li>{@link #remove}：作物（熟不熟都算）按方块战利品表全收进库藏，耕地层的耕地、水、土径换回泥土，撤这块田的强加载。
 *       </li>
 *   <li>作物只认 {@link #CROPS}（小麦、胡萝卜、马铃薯、甜菜根），种子也只认这四种对应的物品；只在标准田里清点与收割。地
 *       狱疣要灵魂沙田，本阶段不做。</li>
 *   <li>{@link #harvest}：成熟作物按方块战利品表掉落，从中扣 1 份种子补种（掉落里没有就从库藏扣，库藏也没有就留空），其
 *       余进库藏。每块田收割后再补空格：空耕地（被踩成泥土的先重新耕好）种这块田现有最多的作物，田里一株没有就取开垦作
 *       物，从库藏扣到没有为止。一次至多处理 {@code fieldLimit} 块田，有收割或补种的田才算一块，整块田一次做完。</li>
 *   <li>⚠ 强加载用 NeoForge {@link #TICKETS} 的带刻工单：原版 {@code /forceload} 的区块附近没有玩家时不随机刻，作物不
 *       长。工单以 {@link FieldPlot#center} 为主人，NeoForge 跨重启保存；失主时 {@link #release} 撤掉，认主时
 *       {@link #retain} 重挂。读档时校验回调把无主福地（或已不是福地的维度）的工单全部丢掉。</li>
 *   <li>本文件有意超过 300 行：开垦整平、拆田、收割补种与强加载工单都围绕同一块 {@link FieldPlot} 的布局，集中维护便于
 *       对照几何。</li>
 * </ul>
 */

public final class FarmWork {

    private FarmWork() {}

    public static final TicketController TICKETS = new TicketController(Guzhenren.id("blissful_land_fields"),
            FarmWork::validateTickets);
    public static final int FILL_DEPTH = 8;
    public static final int CLEAR_HEIGHT = 32;
    public static final int RECLAIM_COOLDOWN_TICKS = 60 * Ticks.SECOND;
    public static final List<Block> CROPS = List.of(Blocks.WHEAT, Blocks.CARROTS, Blocks.POTATOES, Blocks.BEETROOTS);
    private static final int NO_GROUND = Integer.MIN_VALUE;

    public record Count(int mature, int growing) {}

    public record Harvest(int reaped, int sown, int fields) {}

    public static long reclaimCooldown(@NotNull ServerLevel level, @NotNull BlissfulLandData data) {
        return Math.max(0L, data.farm().reclaimReadyAt() - level.getGameTime());
    }

    //region Reclaim [开垦与拆田]
    public static @Nullable FieldPlot reclaim(@NotNull ServerLevel level, @NotNull BlissfulLandData data,
                                              @NotNull FieldPlot plot, @NotNull ServerPlayer owner) {
        if (!ModEvents.canGrief(level, owner)) return null;

        for (ChunkPos chunk : plot.chunks()) level.getChunk(chunk.x, chunk.z);
        int[] grounds = new int[plot.size() * plot.size()];
        int index = 0;
        for (int x = plot.minX(); x <= plot.maxX(); x++) {
            for (int z = plot.minZ(); z <= plot.maxZ(); z++) {
                int ground = groundY(level, x, z);
                if (ground == NO_GROUND) return null;
                grounds[index++] = ground;
            }
        }
        FieldPlot field = plot.atY(FieldPlot.layer(grounds));
        for (int x = field.minX(); x <= field.maxX(); x++) {
            for (int z = field.minZ(); z <= field.maxZ(); z++) {
                level(level, new BlockPos(x, field.y(), z), field.cell(x, z));
            }
        }
        sowAll(level, data, field.cells());
        data.farm().addField(field);
        data.farm().setReclaimReadyAt(level.getGameTime() + RECLAIM_COOLDOWN_TICKS);
        setForced(level, field, true);
        return field;
    }

    private static void sowAll(ServerLevel level, BlissfulLandData data, List<BlockPos> cells) {
        ItemStack seed = data.storage().first(seedFilter(data.farm().crop()));
        if (seed.isEmpty()) return;
        Predicate<ItemStack> same = stack -> ItemStack.isSameItemSameComponents(stack, seed);
        int planted = data.storage().consume(same, cells.size());
        BlockState crop = ((BlockItem) seed.getItem()).getBlock().defaultBlockState();
        for (int i = 0; i < planted; i++) level.setBlock(cells.get(i), crop, Block.UPDATE_ALL);
    }

    private static int groundY(ServerLevel level, int x, int z) {
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos(x, level.getHeight(Heightmap.Types.WORLD_SURFACE, x,
                z) - 1, z);
        for (; pos.getY() > level.getMinBuildHeight(); pos.move(0, -1, 0)) {
            BlockState state = level.getBlockState(pos);
            if (!state.getFluidState().isEmpty()) return NO_GROUND;
            if (!MapScanner.isPassable(state)) return pos.getY();
        }
        return NO_GROUND;
    }

    private static void level(ServerLevel level, BlockPos ground, FieldPlot.Cell cell) {
        int top = Math.min(level.getHeight(Heightmap.Types.WORLD_SURFACE, ground.getX(), ground.getZ()),
                ground.getY() + CLEAR_HEIGHT);
        for (int y = top; y > ground.getY(); y--) {
            BlockPos pos = new BlockPos(ground.getX(), y, ground.getZ());
            if (level.getBlockState(pos).isAir()) continue;
            level.setBlock(pos, Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
        }
        for (int depth = 1; depth <= FILL_DEPTH; depth++) {
            BlockPos pos = ground.below(depth);
            if (!level.getBlockState(pos).canBeReplaced()) break;
            level.setBlock(pos, Blocks.DIRT.defaultBlockState(), Block.UPDATE_ALL);
        }
        BlockState surface = switch (cell) {
            case PATH -> Blocks.DIRT_PATH.defaultBlockState();
            case WATER -> Blocks.WATER.defaultBlockState();
            case FARMLAND -> Blocks.FARMLAND.defaultBlockState().setValue(FarmBlock.MOISTURE, FarmBlock.MAX_MOISTURE);
        };
        level.setBlock(ground, surface, Block.UPDATE_ALL);
    }

    public static @Nullable Integer remove(@NotNull ServerLevel level, @NotNull BlissfulLandData data,
                                           @NotNull FieldPlot field, @NotNull ServerPlayer owner) {
        if (!ModEvents.canGrief(level, owner)) return null;

        for (ChunkPos chunk : field.chunks()) level.getChunk(chunk.x, chunk.z);
        int reaped = 0;
        for (BlockPos pos : field.cells()) {
            BlockState state = level.getBlockState(pos);
            if (!isCrop(state)) continue;
            for (ItemStack drop : Block.getDrops(state, level, pos, null)) data.storage().insert(drop);
            level.setBlock(pos, Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
            reaped++;
        }
        for (int x = field.minX(); x <= field.maxX(); x++) {
            for (int z = field.minZ(); z <= field.maxZ(); z++) {
                BlockPos pos = new BlockPos(x, field.y(), z);
                BlockState state = level.getBlockState(pos);
                if (state.is(Blocks.FARMLAND) || state.is(Blocks.WATER) || state.is(Blocks.DIRT_PATH)) {
                    level.setBlock(pos, Blocks.DIRT.defaultBlockState(), Block.UPDATE_ALL);
                }
            }
        }
        setForced(level, field, false);
        data.farm().removeField(field);
        return reaped;
    }

    public static boolean isSeed(@NotNull ItemStack stack) {
        return stack.getItem() instanceof BlockItem item && CROPS.contains(item.getBlock());
    }

    public static @NotNull Predicate<ItemStack> seedFilter(int crop) {
        if (crop == FarmState.AUTO_CROP) return FarmWork::isSeed;
        Item seed = CROPS.get(crop).asItem();
        return stack -> stack.is(seed);
    }
    //endregion

    //region Crops [作物]
    public static boolean isCrop(@NotNull BlockState state) { return CROPS.contains(state.getBlock()); }

    public static boolean isMature(@NotNull BlockState state) {
        return state.getBlock() instanceof CropBlock crop && crop.isMaxAge(state);
    }

    public static @NotNull Map<Block, Count> census(@NotNull ServerLevel level, @NotNull BlissfulLandData data) {
        Map<Block, int[]> counts = new LinkedHashMap<>();
        for (FieldPlot field : data.farm().fields()) {
            if (!level.isLoaded(field.center())) continue;
            for (BlockPos pos : field.cells()) {
                BlockState state = level.getBlockState(pos);
                if (!isCrop(state)) continue;
                int[] count = counts.computeIfAbsent(state.getBlock(), block -> new int[2]);
                count[isMature(state) ? 0 : 1]++;
            }
        }
        Map<Block, Count> census = new LinkedHashMap<>();
        counts.forEach((block, count) -> census.put(block, new Count(count[0], count[1])));
        return census;
    }

    public static @NotNull Count count(@NotNull ServerLevel level, @NotNull FieldPlot field) {
        if (!level.isLoaded(field.center())) return new Count(0, 0);
        int mature = 0;
        int growing = 0;
        for (BlockPos pos : field.cells()) {
            BlockState state = level.getBlockState(pos);
            if (!isCrop(state)) continue;
            if (isMature(state)) mature++;
            else growing++;
        }
        return new Count(mature, growing);
    }

    public static @NotNull Harvest harvest(@NotNull ServerLevel level, @NotNull BlissfulLandData data, int fieldLimit) {
        int reaped = 0;
        int sown = 0;
        int fields = 0;
        for (FieldPlot field : data.farm().fields()) {
            if (fields >= fieldLimit) break;
            if (!level.isLoaded(field.center())) continue;
            List<BlockPos> cells = field.cells();
            int before = reaped + sown;
            for (BlockPos pos : cells) {
                BlockState state = level.getBlockState(pos);
                if (!isCrop(state) || !isMature(state)) continue;

                reap(level, data, pos, state);
                reaped++;
            }
            sown += sow(level, data, cells);
            if (reaped + sown > before) fields++;
        }
        return new Harvest(reaped, sown, fields);
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

    private static int sow(ServerLevel level, BlissfulLandData data, List<BlockPos> cells) {
        Block crop = cropOf(level, cells);
        if (crop == null) {
            ItemStack seed = data.storage().first(seedFilter(data.farm().crop()));
            if (seed.isEmpty()) return 0;
            crop = ((BlockItem) seed.getItem()).getBlock();
        }
        Item seed = crop.asItem();
        int sown = 0;
        for (BlockPos pos : cells) {
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
    //endregion

    //region Tickets [强加载]
    public static void retain(@NotNull ServerLevel level, @NotNull BlissfulLandData data) {
        for (FieldPlot field : data.farm().fields()) setForced(level, field, true);
    }

    public static void release(@NotNull ServerLevel level, @NotNull BlissfulLandData data) {
        for (FieldPlot field : data.farm().fields()) setForced(level, field, false);
    }

    public static void setForced(@NotNull ServerLevel level, @NotNull FieldPlot field, boolean add) {
        BlockPos owner = field.center();
        for (ChunkPos chunk : field.chunks()) TICKETS.forceChunk(level, owner, chunk.x, chunk.z, add, true);
    }

    private static void validateTickets(ServerLevel level, TicketHelper tickets) {
        BlissfulLand land = Lands.landOf(level);
        if (land != null && BlissfulLandData.get(level.getServer(), land).hasOwner()) return;
        for (BlockPos owner : List.copyOf(tickets.getBlockTickets().keySet())) tickets.removeAllTickets(owner);
    }
    //endregion
}
