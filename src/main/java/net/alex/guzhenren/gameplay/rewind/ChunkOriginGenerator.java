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
 * 从世界种子在后台重新生成区块初始裸地形的生成器（供山如故[Mountain As Before Gu]与江如故[River As Before Gu]使用）。
 * <ul>
 *   <li>在绝不挂载到真实维度的临时 {@link ProtoChunk} 上运行维度自身的区块生成器（依次执行
 *       {@link ChunkPyramid#GENERATION_PYRAMID} 的 {@code BIOMES}、{@code NOISE}、{@code SURFACE}、{@code CARVERS} 与
 *       {@code FEATURES}），完整复现模组地形（Gu World、Tectonic、Terralith）；因初始状态跳过结构起点生成，不会放置任何
 *       结构建筑，也不会受村庄等结构的地形平整影响。</li>
 *   <li>地物装饰步（{@code FEATURES}）仅用于保留悬崖花岗岩/方解石岩层等岩石上色：{@link #keep} 仅允许同属
 *       {@link ModBlockTags#AS_BEFORE_TERRAIN} 的地形方块互换（将装饰步生成的树木、植物、湖泊、巨石与冰雪退回雕刻后的裸
 *       地形），并将属于 {@link ModBlockTags#AS_BEFORE_STRIPPED} 的矿石与原矿脉替换为所在高度的普通石头、深板岩或下界
 *       岩。</li>
 *   <li>因地物装饰会向相邻 1 区块写入、地形生成需读取外围群系，生成半径 {@code r} 的目标方阵需外扩铺设 {@code r + 3} 的
 *       临时区块（例如 5×5 目标方阵需铺设 11×11 临时区块）；{@code layOut} 在主线程捕获上下文后交由
 *       {@code Util.backgroundExecutor()} 异步执行（地形四步并行，地物装饰逐块串行）。</li>
 *   <li>私有 {@code ScratchHolder} 对任意请求状态直接返回内存中的临时区块而不触发区块调度；生成的裸区块仅含方块状态，不
 *       含光照、实体、计划刻或方块实体。</li>
 *   <li>⚠ 底层世界生成管线（{@code GenerationChunkHolder}、{@code ChunkStep}、{@code WorldGenContext}）仅封装在本类
 *       内；生成结果反映当前种子与当前世界生成器配置，若存档中途更换过地形模组则会还原出新生成器的地形。</li>
 *   <li>⚠ {@code layOut} 为 {@code WorldGenContext} 的主线程邮箱传入 {@code null}，因直至 {@code FEATURES} 的步骤均不
 *       向该邮箱投递任务。</li>
 * </ul>
 */

public final class ChunkOriginGenerator {

    private ChunkOriginGenerator() {}

    private static final int FEATURE_MARGIN = 1;
    private static final int TERRAIN_MARGIN = 2;
    private static final int BIOME_MARGIN = 3;
    private static final int STRUCTURE_REACH = 8;
    private static final List<ChunkStatus> TERRAIN_STEPS =
            List.of(ChunkStatus.BIOMES, ChunkStatus.NOISE, ChunkStatus.SURFACE, ChunkStatus.CARVERS);

    /** 在后台线程异步生成以 {@code center} 为中心、半径 {@code radius} 方阵的初始裸地形区块映射。 */
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
