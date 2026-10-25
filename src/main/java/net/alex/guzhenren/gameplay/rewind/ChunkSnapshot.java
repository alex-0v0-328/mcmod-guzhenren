package net.alex.guzhenren.gameplay.rewind;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.chunk.LevelChunkSection;
import net.minecraft.world.level.chunk.PalettedContainer;
import org.jetbrains.annotations.NotNull;

/**
 * One chunk as it stood at one game moment: every section's block states and every block entity's saved tag.
 *
 * <p>The states are copies of the sections' own {@link PalettedContainer}s, so a snapshot costs what the chunk's
 * palettes cost (a surface chunk runs some 20 to 30 KB; an all-air section is one palette entry) and serializes
 * through the same codec the chunk file uses. The block entities are {@code saveWithFullMetadata} tags, position and
 * id included, so a restore can match each to the block it belongs to.
 *
 * <p>⚠ Entities, items on the ground and the chunk's biomes are not part of a snapshot: an As Before Gu [如故蛊]
 * puts back the land, not what walked on it. ⚠ A container's items ride along in its tag and are never put back
 * ({@code ChunkRewindService} empties every restored container): a chest restored full would mint items.
 *
 * @author Alex
 * @version 1.0.0
 * @see ChunkRewindService
 * @since 1.0.0
 */

public record ChunkSnapshot(long takenAt, int minSection, List<PalettedContainer<BlockState>> sections,
                            List<CompoundTag> blockEntities) {

    public static final Codec<PalettedContainer<BlockState>> SECTION_CODEC = PalettedContainer.codecRW(
            Block.BLOCK_STATE_REGISTRY, BlockState.CODEC, PalettedContainer.Strategy.SECTION_STATES,
            Blocks.AIR.defaultBlockState());
    public static final Codec<ChunkSnapshot> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.LONG.fieldOf("taken_at").forGetter(ChunkSnapshot::takenAt),
            Codec.INT.fieldOf("min_section").forGetter(ChunkSnapshot::minSection),
            SECTION_CODEC.listOf().fieldOf("sections").forGetter(ChunkSnapshot::sections),
            CompoundTag.CODEC.listOf().optionalFieldOf("block_entities", List.of())
                    .forGetter(ChunkSnapshot::blockEntities)
    ).apply(instance, ChunkSnapshot::new));

    public static @NotNull ChunkSnapshot capture(@NotNull LevelChunk chunk, long now) {
        LevelChunkSection[] sections = chunk.getSections();
        List<PalettedContainer<BlockState>> states = new ArrayList<>(sections.length);
        for (LevelChunkSection section : sections) states.add(section.getStates().copy());

        List<CompoundTag> entities = new ArrayList<>(chunk.getBlockEntities().size());
        for (BlockEntity entity : chunk.getBlockEntities().values()) {
            entities.add(entity.saveWithFullMetadata(chunk.getLevel().registryAccess()));
        }
        return new ChunkSnapshot(now, chunk.getMinSection(), List.copyOf(states), List.copyOf(entities));
    }
}
