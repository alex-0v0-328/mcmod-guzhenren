package net.alex.guzhenren.gameplay.immortalaperture.farm;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;
import net.alex.guzhenren.Guzhenren;
import net.alex.guzhenren.core.ModEvents;
import net.alex.guzhenren.gameplay.immortalaperture.state.BlissfulLand;
import net.alex.guzhenren.gameplay.immortalaperture.state.BlissfulLandData;
import net.alex.guzhenren.gameplay.immortalaperture.state.BlissfulLandLookup;
import net.alex.guzhenren.gameplay.immortalaperture.state.LandCensus;
import net.alex.guzhenren.gameplay.immortalaperture.state.LandRules;
import net.alex.guzhenren.gameplay.immortalaperture.storage.BlissfulStorage;
import net.alex.guzhenren.registry.block.ModBlocks;
import net.alex.guzhenren.registry.item.ModItems;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
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
 * 地灵面板的标准田：开垦、拆田、改田型、按作物清点、种植、清除作物、收割并补种，以及田的强加载。选址与规划队列在 {@link FieldPlans}。
 * <ul>
 *   <li>{@link #reclaim}：先同步加载田所在区块；每列从地表往下找真实地面（跳过 {@link LandRules#isPassable}），任一列先
 *       碰到流体就不开（选址已查过，这里只防开垦前地形变了）；以 {@link FieldPlot#layer}（列数最多的地面层）为耕地层，
 *       把整块田整平（上方清空至地表、至多 {@value LandRules#CLEAR_HEIGHT} 格，树一并清掉；下方向下补土至多
 *       {@value LandRules#FILL_DEPTH} 格），按 {@link FieldPlot} 铺土径、水与耕地，记账（作物未选，普通田），不播种：开垦 → 选作物
 *       → 种植（Alex 定）。改地形先过 {@link ModEvents#canGrief}。</li>
 *   <li>⚠ {@link #reclaim} 清整不掉落任何方块，田上玩家搭的普通方块会被直接抹掉；有方块实体的区块选址时已拒。</li>
 *   <li>{@link #remove}：作物（熟不熟都算）按方块战利品表全收进库藏，耕地层的耕地（含黑土耕地）、水、土径换回泥土，撤这
 *       块田的强加载；腐泥冻土不返还。</li>
 *   <li>{@link #retype}：改蛊植田一次从库藏扣 {@link FieldPlot#farmland()} 个腐泥冻土（不够就不改），耕地层的耕地与被踩
 *       的泥土全换黑土耕地；改回普通田黑土换原版耕地、不返还。湿润度与作物都保留。</li>
 *   <li>⚠ 换耕地层与往空格播种都带 {@code UPDATE_KNOWN_SHAPE}：否则相邻作物重新校验存活，亮度不足 8 的直接掉落（同
 *       {@code RottenFrozenMudItem}）；刚整平切出坡体或刚清掉树冠的格子当刻光照还没重算（仍是 0），不带就会种一株弹一株。</li>
 *   <li>作物只认 {@link FarmRules#crops()}（小麦、胡萝卜、马铃薯、甜菜根、知心草），种子也只认对应的物品；只在标准田里清点
 *       与收割。地狱疣要灵魂沙田，本阶段不做。</li>
 *   <li>{@link #harvest}：成熟作物按方块战利品表掉落，补种这块田的作物，从掉落里扣 1 份种子（没有就从库藏扣，库藏也没有
 *       就留空），其余进库藏；未选作物的田只收不种。每块田收割后再补空格：空耕地（被踩成泥土的先重新耕成原版耕地）种这块田
 *       的作物（未选不种），从库藏扣到没有为止；蛊植
 *       田最后把原版耕地逐格掺回黑土，每格扣 1 个腐泥冻土，扣完为止。一次至多处理 {@code fieldLimit} 块田，有收割、补种或
 *       掺土的田才算一块，整块田一次做完。</li>
 *   <li>{@link #sow}：地图上选中田「种植」，按各田的作物像收割后那样补满空格（同 {@link #topUp}，但一次补完所选各块）；
 *       田里有别的作物就不能改种（{@link #accepts}），要先 {@link #uproot}「清除作物」：熟不熟都按方块战利品表收进库藏，
 *       耕地留着，作物重置为未选（不重置的话开面板每秒补种会马上种回原作物）；
 *       {@link #ripen}：选中田「催熟」，每株未熟作物从库藏扣 1 份骨粉直接长到满龄（Alex 定，不走原版随机），骨粉不够
 *       就按田序催熟到用完为止；{@link #harvest} 另有只收所选田的重载。</li>
 *   <li>{@link #topUp}：主人开着面板时每秒调一次，按田序轮流找下一块有空格可补（或可掺土）的田，像收割后那样补满这一块；
 *       轮到哪块记在 {@link FarmState#topUpCursor}（不存档）。{@link #look} 给地图选区卡的熟 / 未熟株数与生长进度
 *       （全田作物龄之和 ÷ 满龄之和）。</li>
 *   <li>⚠ 强加载用 NeoForge {@link #TICKETS} 的带刻工单：原版 {@code /forceload} 的区块附近没有玩家时不随机刻，作物不
 *       长。工单以 {@link FieldPlot#center} 为主人，NeoForge 跨重启保存；失主时 {@link #release} 撤掉，认主时
 *       {@link #retain} 重挂。读档时校验回调把无主福地（或已不是福地的维度）的工单全部丢掉。</li>
 *   <li>本文件有意超过 300 行：开垦整平、拆田、收割补种与强加载工单都围绕同一块 {@link FieldPlot} 的布局，集中维护便于
 *       对照几何。</li>
 * </ul>
 */

public final class FarmWork {

    private FarmWork() {}

    public static final LandCensus.Census<Map<Block, Count>> CENSUS = LandCensus.Census.ofLand(FarmWork::census);
    public static final TicketController TICKETS = new TicketController(Guzhenren.id("blissful_land_fields"),
            FarmWork::validateTickets);
    private static final int NO_GROUND = Integer.MIN_VALUE;

    public record Count(int mature, int growing) {}

    /** 地图选区卡用：熟 / 未熟株数、生长进度（%）、田里株数最多的作物（{@link FarmRules#crops()} 下标，一株没有为 {@link FarmState#NO_CROP}）。 */
    public record Look(int mature, int growing, int growth, int main) {}

    public record Harvest(int reaped, int sown, int fields) {}

    public record Ripen(int ripened, int unripe) {}

    public enum Retype { CHANGED, SHORT, DENIED }

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
        FarmState.of(data).addField(field);
        setForced(level, field, true);
        return field;
    }

    private static void plant(ServerLevel level, BlockPos cell, BlockState crop) {
        level.setBlock(cell, crop, Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE);
    }

    private static int groundY(ServerLevel level, int x, int z) {
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos(x, level.getHeight(Heightmap.Types.WORLD_SURFACE, x,
                z) - 1, z);
        for (; pos.getY() > level.getMinBuildHeight(); pos.move(0, -1, 0)) {
            BlockState state = level.getBlockState(pos);
            if (!state.getFluidState().isEmpty()) return NO_GROUND;
            if (!LandRules.isPassable(state)) return pos.getY();
        }
        return NO_GROUND;
    }

    private static void level(ServerLevel level, BlockPos ground, FieldPlot.Cell cell) {
        int top = Math.min(level.getHeight(Heightmap.Types.WORLD_SURFACE, ground.getX(), ground.getZ()),
                ground.getY() + LandRules.CLEAR_HEIGHT);
        for (int y = top; y > ground.getY(); y--) {
            BlockPos pos = new BlockPos(ground.getX(), y, ground.getZ());
            if (level.getBlockState(pos).isAir()) continue;
            level.setBlock(pos, Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
        }
        for (int depth = 1; depth <= LandRules.FILL_DEPTH; depth++) {
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
            for (ItemStack drop : Block.getDrops(state, level, pos, null)) BlissfulStorage.of(data).insert(drop);
            level.setBlock(pos, Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
            reaped++;
        }
        for (int x = field.minX(); x <= field.maxX(); x++) {
            for (int z = field.minZ(); z <= field.maxZ(); z++) {
                BlockPos pos = new BlockPos(x, field.y(), z);
                BlockState state = level.getBlockState(pos);
                if (isSoil(state) || state.is(Blocks.WATER) || state.is(Blocks.DIRT_PATH)) {
                    level.setBlock(pos, Blocks.DIRT.defaultBlockState(), Block.UPDATE_ALL);
                }
            }
        }
        setForced(level, field, false);
        FarmState.of(data).removeField(field);
        return reaped;
    }

    public static boolean isSeed(@NotNull ItemStack stack) {
        return stack.getItem() instanceof BlockItem item && FarmRules.crops().contains(item.getBlock());
    }

    public static @NotNull Predicate<ItemStack> seedFilter(int crop) {
        Item seed = FarmRules.crops().get(crop).asItem();
        return stack -> stack.is(seed);
    }

    public static boolean isMud(@NotNull ItemStack stack) { return stack.is(ModItems.ROTTEN_FROZEN_MUD.get()); }

    public static boolean isBoneMeal(@NotNull ItemStack stack) { return stack.is(Items.BONE_MEAL); }

    private static boolean isSoil(BlockState state) {
        return state.is(Blocks.FARMLAND) || state.is(ModBlocks.BLACK_SOIL_FARMLAND.get());
    }
    //endregion

    //region Kind [田型]
    public static @NotNull Retype retype(@NotNull ServerLevel level, @NotNull BlissfulLandData data,
                                         @NotNull FieldPlot field, @NotNull FieldKind kind,
                                         @NotNull ServerPlayer owner) {
        if (!ModEvents.canGrief(level, owner)) return Retype.DENIED;
        FarmState state = FarmState.of(data);
        if (state.kind(field) == kind) return Retype.CHANGED;
        BlissfulStorage storage = BlissfulStorage.of(data);
        if (kind == FieldKind.GU_PLANT) {
            if (storage.count(FarmWork::isMud) < field.farmland()) return Retype.SHORT;
            storage.consume(FarmWork::isMud, field.farmland());
        }

        for (ChunkPos chunk : field.chunks()) level.getChunk(chunk.x, chunk.z);
        for (BlockPos cell : field.cells()) {
            BlockState ground = level.getBlockState(cell.below());
            if (isSoil(ground) || ground.is(Blocks.DIRT)) setSoil(level, cell.below(), ground, kind.soil());
        }
        state.setKind(field, kind);
        return Retype.CHANGED;
    }

    private static void setSoil(ServerLevel level, BlockPos pos, BlockState before, Block soil) {
        int moisture = before.hasProperty(FarmBlock.MOISTURE) ? before.getValue(FarmBlock.MOISTURE)
                : FarmBlock.MAX_MOISTURE;
        level.setBlock(pos, soil.defaultBlockState().setValue(FarmBlock.MOISTURE, moisture),
                Block.UPDATE_ALL | Block.UPDATE_KNOWN_SHAPE);
    }
    //endregion

    //region Crops [作物]
    public static boolean isCrop(@NotNull BlockState state) { return FarmRules.crops().contains(state.getBlock()); }

    public static boolean isMature(@NotNull BlockState state) {
        return state.getBlock() instanceof CropBlock crop && crop.isMaxAge(state);
    }

    public static @NotNull Map<Block, Count> census(@NotNull ServerLevel level, @NotNull BlissfulLandData data) {
        Map<Block, int[]> counts = new LinkedHashMap<>();
        for (FieldPlot field : FarmState.of(data).fields()) {
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

    public static @NotNull Look look(@NotNull ServerLevel level, @NotNull FieldPlot field) {
        if (!level.isLoaded(field.center())) return new Look(0, 0, 0, FarmState.NO_CROP);
        int mature = 0;
        int growing = 0;
        long age = 0L;
        long full = 0L;
        int[] kinds = new int[FarmRules.crops().size()];
        for (BlockPos pos : field.cells()) {
            BlockState state = level.getBlockState(pos);
            if (!isCrop(state)) continue;
            if (isMature(state)) mature++;
            else growing++;
            kinds[FarmRules.crops().indexOf(state.getBlock())]++;
            if (state.getBlock() instanceof CropBlock crop) {
                age += crop.getAge(state);
                full += crop.getMaxAge();
            }
        }
        int main = FarmState.NO_CROP;
        for (int crop = 0; crop < kinds.length; crop++) {
            if (kinds[crop] > 0 && (main == FarmState.NO_CROP || kinds[crop] > kinds[main])) main = crop;
        }
        return new Look(mature, growing, full == 0L ? 0 : (int) (age * 100L / full), main);
    }

    public static @NotNull Harvest harvest(@NotNull ServerLevel level, @NotNull BlissfulLandData data, int fieldLimit) {
        return harvest(level, data, FarmState.of(data).fields(), fieldLimit);
    }

    public static @NotNull Harvest harvest(@NotNull ServerLevel level, @NotNull BlissfulLandData data,
                                           @NotNull List<FieldPlot> picked, int fieldLimit) {
        FarmState farm = FarmState.of(data);
        int reaped = 0;
        int sown = 0;
        int fields = 0;
        for (FieldPlot field : picked) {
            if (fields >= fieldLimit) break;
            if (!level.isLoaded(field.center())) continue;
            List<BlockPos> cells = field.cells();
            Block chosen = chosen(farm, field);
            int before = reaped + sown;
            for (BlockPos pos : cells) {
                BlockState state = level.getBlockState(pos);
                if (!isCrop(state) || !isMature(state)) continue;

                reap(level, data, pos, state, chosen);
                reaped++;
            }
            int filled = fill(level, data, field);
            sown += Math.max(0, filled);
            if (reaped + sown > before || filled < 0) fields++;
        }
        return new Harvest(reaped, sown, fields);
    }

    public static int sow(@NotNull ServerLevel level, @NotNull BlissfulLandData data,
                          @NotNull List<FieldPlot> picked) {
        int sown = 0;
        for (FieldPlot field : picked) {
            if (level.isLoaded(field.center())) sown += Math.max(0, fill(level, data, field));
        }
        return sown;
    }

    /** 这块田能不能种 {@code crop}：田里没有别的作物（同种的株照留）。 */
    public static boolean accepts(@NotNull ServerLevel level, @NotNull FieldPlot field, int crop) {
        Block wanted = FarmRules.crops().get(crop);
        for (BlockPos pos : field.cells()) {
            BlockState state = level.getBlockState(pos);
            if (isCrop(state) && !state.is(wanted)) return false;
        }
        return true;
    }

    /** 清除所选各田的作物：熟不熟都按方块战利品表收进库藏，耕地留着，作物重置为未选；返回清掉的株数。 */
    public static int uproot(@NotNull ServerLevel level, @NotNull BlissfulLandData data,
                             @NotNull List<FieldPlot> picked) {
        FarmState farm = FarmState.of(data);
        int cleared = 0;
        for (FieldPlot field : picked) {
            if (!level.isLoaded(field.center())) continue;
            for (BlockPos pos : field.cells()) {
                BlockState state = level.getBlockState(pos);
                if (!isCrop(state)) continue;
                for (ItemStack drop : Block.getDrops(state, level, pos, null)) BlissfulStorage.of(data).insert(drop);
                level.setBlock(pos, Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
                cleared++;
            }
            farm.setCrop(field, FarmState.NO_CROP);
        }
        return cleared;
    }

    public static @NotNull Ripen ripen(@NotNull ServerLevel level, @NotNull BlissfulLandData data,
                                       @NotNull List<FieldPlot> picked) {
        List<BlockPos> unripe = new ArrayList<>();
        for (FieldPlot field : picked) {
            if (!level.isLoaded(field.center())) continue;
            for (BlockPos pos : field.cells()) {
                BlockState state = level.getBlockState(pos);
                if (isCrop(state) && !isMature(state)) unripe.add(pos);
            }
        }
        int paid = unripe.isEmpty() ? 0 : BlissfulStorage.of(data).consume(FarmWork::isBoneMeal, unripe.size());
        for (int i = 0; i < paid; i++) {
            BlockPos pos = unripe.get(i);
            CropBlock crop = (CropBlock) level.getBlockState(pos).getBlock();
            level.setBlock(pos, crop.getStateForAge(crop.getMaxAge()), Block.UPDATE_CLIENTS);
        }
        return new Ripen(paid, unripe.size());
    }

    public static int topUp(@NotNull ServerLevel level, @NotNull BlissfulLandData data) {
        FarmState farm = FarmState.of(data);
        List<FieldPlot> fields = farm.fields();
        if (fields.isEmpty()) return 0;
        int start = Math.floorMod(farm.topUpCursor(), fields.size());
        for (int step = 0; step < fields.size(); step++) {
            int at = (start + step) % fields.size();
            FieldPlot field = fields.get(at);
            if (!level.isLoaded(field.center())) continue;
            int filled = fill(level, data, field);
            if (filled == 0) continue;
            farm.setTopUpCursor(at + 1);
            return Math.max(0, filled);
        }
        return 0;
    }

    private static int fill(ServerLevel level, BlissfulLandData data, FieldPlot field) {
        FarmState farm = FarmState.of(data);
        List<BlockPos> cells = field.cells();
        Block chosen = chosen(farm, field);
        int sown = chosen == null ? 0 : sow(level, data, cells, chosen);
        int mixed = farm.kind(field) == FieldKind.GU_PLANT ? mix(level, data, cells) : 0;
        return sown > 0 || mixed == 0 ? sown : -1;
    }

    private static @Nullable Block chosen(FarmState farm, FieldPlot field) {
        int crop = farm.crop(field);
        return crop == FarmState.NO_CROP ? null : FarmRules.crops().get(crop);
    }

    private static void reap(ServerLevel level, BlissfulLandData data, BlockPos pos, BlockState state,
                             @Nullable Block next) {
        boolean replanted = false;
        for (ItemStack drop : Block.getDrops(state, level, pos, null)) {
            if (!replanted && next != null && drop.is(next.asItem())) {
                drop.shrink(1);
                replanted = true;
            }
            if (!drop.isEmpty()) BlissfulStorage.of(data).insert(drop);
        }
        if (next == null) {
            level.setBlock(pos, Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
            return;
        }
        if (!replanted) replanted = BlissfulStorage.of(data).consume(stack -> stack.is(next.asItem()), 1) == 1;
        level.setBlock(pos, replanted ? next.defaultBlockState() : Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
    }

    private static int sow(ServerLevel level, BlissfulLandData data, List<BlockPos> cells, Block crop) {
        Item seed = crop.asItem();
        int sown = 0;
        for (BlockPos pos : cells) {
            BlockState ground = level.getBlockState(pos.below());
            if (!level.getBlockState(pos).isAir() || !isSoil(ground) && !ground.is(Blocks.DIRT)) continue;
            if (BlissfulStorage.of(data).consume(stack -> stack.is(seed), 1) == 0) break;

            if (ground.is(Blocks.DIRT)) {
                level.setBlock(pos.below(), Blocks.FARMLAND.defaultBlockState()
                        .setValue(FarmBlock.MOISTURE, FarmBlock.MAX_MOISTURE), Block.UPDATE_ALL);
            }
            plant(level, pos, crop.defaultBlockState());
            sown++;
        }
        return sown;
    }

    private static int mix(ServerLevel level, BlissfulLandData data, List<BlockPos> cells) {
        Block soil = FieldKind.GU_PLANT.soil();
        int mixed = 0;
        for (BlockPos pos : cells) {
            BlockState ground = level.getBlockState(pos.below());
            if (!ground.is(Blocks.FARMLAND)) continue;
            if (BlissfulStorage.of(data).consume(FarmWork::isMud, 1) == 0) break;

            setSoil(level, pos.below(), ground, soil);
            mixed++;
        }
        return mixed;
    }
    //endregion

    //region Tickets [强加载]
    public static int host(@NotNull ServerLevel level, @NotNull BlissfulLandData data, @Nullable ServerPlayer owner) {
        Harvest done = harvest(level, data, FarmRules.HOSTING_FIELDS);
        FarmState.of(data).setLastRound(done.reaped(), done.sown());
        return done.fields();
    }

    public static void retain(@NotNull ServerLevel level, @NotNull BlissfulLandData data) {
        for (FieldPlot field : FarmState.of(data).fields()) setForced(level, field, true);
    }

    public static void release(@NotNull ServerLevel level, @NotNull BlissfulLandData data) {
        for (FieldPlot field : FarmState.of(data).fields()) setForced(level, field, false);
    }

    public static void setForced(@NotNull ServerLevel level, @NotNull FieldPlot field, boolean add) {
        BlockPos owner = field.center();
        for (ChunkPos chunk : field.chunks()) TICKETS.forceChunk(level, owner, chunk.x, chunk.z, add, true);
    }

    private static void validateTickets(ServerLevel level, TicketHelper tickets) {
        BlissfulLand land = BlissfulLandLookup.landOf(level);
        if (land != null && BlissfulLandData.get(level.getServer(), land).hasOwner()) return;
        for (BlockPos owner : List.copyOf(tickets.getBlockTickets().keySet())) tickets.removeAllTickets(owner);
    }
    //endregion
}
