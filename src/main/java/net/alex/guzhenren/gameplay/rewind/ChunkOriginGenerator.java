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
 * Generates the bare land again from the world's seed, into scratch {@link ProtoChunk}s that never join the level:
 * the source the Mountain and River As Before Gu [山如故 / 江如故] restore from. Only the landform comes back -- no
 * tree, plant, lake, snow, ice, ore, structure or building.
 *
 * <p>Runs the vanilla generation steps {@code BIOMES} through {@code FEATURES} of
 * {@link ChunkPyramid#GENERATION_PYRAMID} with the level's own generator, so modded terrain (Gu World [蛊世界],
 * Tectonic, Terralith) comes back the way it was shaped. The scratch chunks carry no structure starts, so no
 * structure is placed and none bends the terrain: a village's hill comes back as the hill before the village.
 * Decoration ({@code FEATURES}) runs only to colour the land -- a ported cliff's granite and calcite bands and a
 * canyon's stone columns are decorations -- and {@link #keep} lets a decorated block through only where it swaps one
 * {@link ModBlockTags#AS_BEFORE_TERRAIN} block for another; anything a decoration adds or removes (trees, plants,
 * lakes, boulders, snow, ice) falls back to the bare terrain. Every {@link ModBlockTags#AS_BEFORE_STRIPPED} block
 * (ores, the noise's raw ore veins) comes back as the stone or deepslate it sits in.
 *
 * <p>Features write one chunk past their own and the terrain steps read biomes one chunk out, so a square of radius
 * {@code r} decorates {@code r + 1}, shapes {@code r + 2} and lays biomes over {@code r + 3}: the 3×3 restore lays
 * out 9×9 scratch chunks. Everything after {@link #layOut} runs on {@code Util.backgroundExecutor()}; terrain steps
 * run in parallel (each writes its own chunk), features one chunk after another (each writes its neighbors). The
 * result carries blocks only: no light, entities, ticks or block entities.
 *
 * <p>⚠ A version seam: {@code GenerationChunkHolder}, {@code ChunkStep} and {@code WorldGenContext} are the 1.21.1
 * generation pipeline. ⚠ The result is the seed's land under today's generator and mods: a world whose generator or
 * worldgen mods changed since a chunk was made gets the new land there.
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

    /**
     * Lays out the scratch chunks on the main thread, then generates the square of {@code radius} around
     * {@code center} in the background. The future holds the bare land of every position in the square.
     */
    public static @NotNull CompletableFuture<Map<ChunkPos, ChunkAccess>> generate(@NotNull ServerLevel level,
                                                                                  @NotNull ChunkPos center,
                                                                                  int radius) {
        Region region = layOut(level, center, radius);
        return CompletableFuture.supplyAsync(() -> region, Util.backgroundExecutor())
                .thenCompose(ChunkOriginGenerator::runTerrain)
                .thenApplyAsync(ChunkOriginGenerator::runFeatures, Util.backgroundExecutor());
    }

    /**
     * The block the land keeps at one position: the decorated block where a decoration swapped terrain for terrain,
     * else the bare terrain, and a stripped block turned back to the stone or deepslate around it.
     */
    public static @NotNull BlockState keep(@NotNull BlockState bare, @NotNull BlockState decorated, int y) {
        boolean recoloured = decorated != bare && bare.is(ModBlockTags.AS_BEFORE_TERRAIN)
                && decorated.is(ModBlockTags.AS_BEFORE_TERRAIN);
        BlockState kept = recoloured ? decorated : bare;
        if (!kept.is(ModBlockTags.AS_BEFORE_STRIPPED)) return kept;
        if (kept.is(Tags.Blocks.ORES_IN_GROUND_NETHERRACK)) return Blocks.NETHERRACK.defaultBlockState();
        if (kept.is(Tags.Blocks.ORES_IN_GROUND_DEEPSLATE) || y < 0) return Blocks.DEEPSLATE.defaultBlockState();
        return Blocks.STONE.defaultBlockState();
    }

    //region laying out -- main thread
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

    //region generating -- background
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

    //region the region the steps see
    /** The scratch chunks and the holders a {@code WorldGenRegion} reads: a scratch chunk, else an empty one. */
    private record Region(WorldGenContext context, StaticCache2D<GenerationChunkHolder> cache, ChunkPos center,
                          int radius, Map<Long, ProtoChunk> scratch) {}

    /** Hands its one chunk to every status a generation step asks for; never schedules anything. */
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
        public synchronized @Nullable ChunkAccess getChunkIfPresentUnchecked(@NotNull ChunkStatus status) {
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
