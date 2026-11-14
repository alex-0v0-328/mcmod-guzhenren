package net.alex.guzhenren.gameplay.rewind;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import net.alex.guzhenren.registry.block.ModBlockTags;
import net.minecraft.Util;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Registry;
import net.minecraft.core.SectionPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.GenerationChunkHolder;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.StaticCache2D;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.LevelChunkSection;
import net.minecraft.world.level.chunk.PalettedContainer;
import net.minecraft.world.level.chunk.ProtoChunk;
import net.minecraft.world.level.chunk.UpgradeData;
import net.minecraft.world.level.chunk.status.ChunkPyramid;
import net.minecraft.world.level.chunk.status.ChunkStatus;
import net.minecraft.world.level.chunk.status.ChunkStep;
import net.minecraft.world.level.chunk.status.WorldGenContext;
import net.neoforged.neoforge.common.Tags;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * 从种子重新生成的裸地形，供山如故[Mountain As Before Gu]、江如故[River As Before Gu]还原。
 * <ul>
 *   <li>生成到从不加入维度的临时 {@link ProtoChunk}，只回地形：无树、植物、湖、雪、冰、矿、结构、建筑；用维度自身生成器
 *       跑 {@link ChunkPyramid#GENERATION_PYRAMID} 的 {@code BIOMES} 至 {@code FEATURES}，模组地形（Gu World、
 *       Tectonic、Terralith）按原样回来；无结构起点，不放结构也不被结构改形：村庄的山回到建村前。</li>
 *   <li>装饰（{@code FEATURES}）只为上色（移植悬崖的花岗岩与方解石条带、峡谷石柱）：{@link #keep} 只放行
 *       {@link ModBlockTags#AS_BEFORE_TERRAIN} 互换，装饰增删的树、植物、湖、巨石、雪、冰退回裸地形；
 *       {@link ModBlockTags#AS_BEFORE_STRIPPED}（矿石、噪声原矿矿脉）还原为所在的石头或深板岩。</li>
 *   <li>地物写到外一区块、地形步骤读外一区块群系，故半径 {@code r} 装饰 {@code r + 1}、塑形 {@code r + 2}、铺群系
 *       {@code r + 3}：5×5 还原铺 11×11 临时区块；{@link #layOut} 之后在 {@code Util.backgroundExecutor()} 运行，地形步
 *       骤并行（各写本区块），地物逐块串行（会写邻居）。</li>
 *   <li>{@link ScratchHolder} 对任何请求状态都交出同一区块（无则空区块），从不调度；结果只含方块，无光照、实体、计划
 *       tick、方块实体。</li>
 *   <li>⚠ 生成管线（{@code GenerationChunkHolder}、{@code ChunkStep}、{@code WorldGenContext}）只在此引用；结果为种子在
 *       当前生成器与模组下的地形，区块生成后换过生成器或世界生成模组，该处得新地形。</li>
 * </ul>
 *
 * @author Alex
 * @version 1.0.0
 * @see ChunkRewindService
 * @since 1.0.0
 */

public final class ChunkOriginGenerator {

    private ChunkOriginGenerator() {}

    private static final int FEATURE_MARGIN = 1;
    private static final int TERRAIN_MARGIN = 2;
    private static final int BIOME_MARGIN = 3;
    private static final int STRUCTURE_REACH = 8;
    private static final List<ChunkStatus> TERRAIN_STEPS =
            List.of(ChunkStatus.BIOMES, ChunkStatus.NOISE, ChunkStatus.SURFACE, ChunkStatus.CARVERS);

    /** {@code center} 周围半径 {@code radius} 方阵的裸地形，后台生成。 */
    public static @NotNull CompletableFuture<Map<ChunkPos, ChunkAccess>> generate(@NotNull ServerLevel level,
                                                                                  @NotNull ChunkPos center,
                                                                                  int radius) {
        Region region = layOut(level, center, radius);
        return CompletableFuture.supplyAsync(() -> region, Util.backgroundExecutor())
                .thenCompose(ChunkOriginGenerator::runTerrain)
                .thenApplyAsync(ChunkOriginGenerator::runFeatures, Util.backgroundExecutor());
    }

    public static @NotNull BlockState keep(@NotNull BlockState bare, @NotNull BlockState decorated, int y) {
        boolean recoloured = decorated != bare && bare.is(ModBlockTags.AS_BEFORE_TERRAIN)
                && decorated.is(ModBlockTags.AS_BEFORE_TERRAIN);
        BlockState kept = recoloured ? decorated : bare;
        if (!kept.is(ModBlockTags.AS_BEFORE_STRIPPED)) return kept;
        if (kept.is(Tags.Blocks.ORES_IN_GROUND_NETHERRACK)) return Blocks.NETHERRACK.defaultBlockState();
        if (kept.is(Tags.Blocks.ORES_IN_GROUND_DEEPSLATE) || y < 0) return Blocks.DEEPSLATE.defaultBlockState();
        return Blocks.STONE.defaultBlockState();
    }

    //region laying out [铺设]
    private static Region layOut(ServerLevel level, ChunkPos center, int radius) {
        Registry<Biome> biomes = level.registryAccess().registryOrThrow(Registries.BIOME);
        int outer = radius + BIOME_MARGIN;
        Map<Long, ProtoChunk> scratch = new HashMap<>();
        for (int x = center.x - outer; x <= center.x + outer; x++) {
            for (int z = center.z - outer; z <= center.z + outer; z++) {
                ProtoChunk chunk = blank(level, biomes, new ChunkPos(x, z));
                chunk.setPersistedStatus(ChunkStatus.STRUCTURE_REFERENCES);
                scratch.put(ChunkPos.asLong(x, z), chunk);
            }
        }
        // No step up to FEATURES posts to the main-thread mailbox.
        //noinspection DataFlowIssue
        WorldGenContext context = new WorldGenContext(level, level.getChunkSource().getGenerator(),
                level.getStructureManager(), level.getChunkSource().getLightEngine(), null);
        StaticCache2D<GenerationChunkHolder> cache = StaticCache2D.create(center.x, center.z,
                outer + STRUCTURE_REACH, (x, z) -> new ScratchHolder(new ChunkPos(x, z),
                        scratch.get(ChunkPos.asLong(x, z)), level, biomes));
        return new Region(context, cache, center, radius, scratch);
    }

    private static ProtoChunk blank(ServerLevel level, Registry<Biome> biomes, ChunkPos pos) {
        return new ProtoChunk(pos, UpgradeData.EMPTY, level, biomes, null);
    }
    //endregion

    //region generating [生成]
    private static CompletableFuture<Region> runTerrain(Region region) {
        CompletableFuture<Void> done = CompletableFuture.completedFuture(null);
        for (ChunkStatus status : TERRAIN_STEPS) {
            done = done.thenCompose(ignored -> {
                ChunkStep step = ChunkPyramid.GENERATION_PYRAMID.getStepTo(status);
                List<CompletableFuture<ChunkAccess>> chunks = new ArrayList<>();
                int reach = region.radius() + (status == ChunkStatus.BIOMES ? BIOME_MARGIN : TERRAIN_MARGIN);
                for (ProtoChunk chunk : region.scratch().values()) {
                    if (chunk.getPos().getChessboardDistance(region.center()) > reach) continue;
                    chunks.add(step.apply(region.context(), region.cache(), chunk));
                }
                return CompletableFuture.allOf(chunks.toArray(CompletableFuture[]::new));
            });
        }
        return done.thenApply(ignored -> region);
    }

    private static Map<ChunkPos, ChunkAccess> runFeatures(Region region) {
        ChunkPos center = region.center();
        int radius = region.radius();
        Map<ChunkPos, List<PalettedContainer<BlockState>>> bare = new HashMap<>();
        for (int x = center.x - radius; x <= center.x + radius; x++) {
            for (int z = center.z - radius; z <= center.z + radius; z++) {
                ProtoChunk chunk = region.scratch().get(ChunkPos.asLong(x, z));
                List<PalettedContainer<BlockState>> states = new ArrayList<>();
                for (LevelChunkSection section : chunk.getSections()) states.add(section.getStates().copy());
                bare.put(chunk.getPos(), states);
            }
        }

        ChunkStep step = ChunkPyramid.GENERATION_PYRAMID.getStepTo(ChunkStatus.FEATURES);
        int reach = radius + FEATURE_MARGIN;
        for (int x = center.x - reach; x <= center.x + reach; x++) {
            for (int z = center.z - reach; z <= center.z + reach; z++) {
                step.apply(region.context(), region.cache(), region.scratch().get(ChunkPos.asLong(x, z))).join();
            }
        }

        Map<ChunkPos, ChunkAccess> result = new HashMap<>();
        bare.forEach((pos, states) -> {
            ProtoChunk chunk = region.scratch().get(pos.toLong());
            strip(chunk, states);
            result.put(pos, chunk);
        });
        return Map.copyOf(result);
    }

    private static void strip(ProtoChunk chunk, List<PalettedContainer<BlockState>> bare) {
        for (int index = 0; index < bare.size(); index++) {
            PalettedContainer<BlockState> was = bare.get(index);
            LevelChunkSection section = chunk.getSection(index);
            int minY = SectionPos.sectionToBlockCoord(chunk.getSectionYFromSectionIndex(index));
            for (int y = 0; y < SectionPos.SECTION_SIZE; y++) {
                for (int z = 0; z < SectionPos.SECTION_SIZE; z++) {
                    for (int x = 0; x < SectionPos.SECTION_SIZE; x++) {
                        BlockState decorated = section.getBlockState(x, y, z);
                        BlockState kept = keep(was.get(x, y, z), decorated, minY + y);
                        if (kept != decorated) section.setBlockState(x, y, z, kept, false);
                    }
                }
            }
        }
        for (BlockPos pos : new HashSet<>(chunk.getBlockEntitiesPos())) chunk.removeBlockEntity(pos);
    }
    //endregion

    //region the region the steps see [步骤视野]
    private record Region(WorldGenContext context, StaticCache2D<GenerationChunkHolder> cache, ChunkPos center,
                          int radius, Map<Long, ProtoChunk> scratch) {}

    private static final class ScratchHolder extends GenerationChunkHolder {

        private @Nullable ChunkAccess chunk;
        private final ServerLevel level;
        private final Registry<Biome> biomes;

        private ScratchHolder(ChunkPos pos, @Nullable ChunkAccess chunk, ServerLevel level, Registry<Biome> biomes) {
            super(pos);
            this.chunk = chunk;
            this.level = level;
            this.biomes = biomes;
        }

        @Override
        public synchronized ChunkAccess getChunkIfPresentUnchecked(@NotNull ChunkStatus status) {
            if (chunk == null) chunk = blank(level, biomes, pos);
            return chunk;
        }

        @Override
        public int getTicketLevel() { return 0; }

        @Override
        public int getQueueLevel() { return 0; }
    }
    //endregion
}
