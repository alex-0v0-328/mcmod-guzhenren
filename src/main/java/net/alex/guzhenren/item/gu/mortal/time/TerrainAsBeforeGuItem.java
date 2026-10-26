package net.alex.guzhenren.item.gu.mortal.time;

import net.alex.guzhenren.core.ActionBar;
import net.alex.guzhenren.core.ModEvents;
import java.util.ArrayList;
import java.util.List;
import net.alex.guzhenren.gameplay.rewind.ChunkKind;
import net.alex.guzhenren.gameplay.rewind.ChunkRewindService;
import net.alex.guzhenren.item.gu.GuSpec;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.chunk.LevelChunk;
import org.jetbrains.annotations.Nullable;

/**
 * Mountain As Before Gu [山如故] and River As Before Gu [江如故]: the chunks around the carrier go back to the bare
 * landform the world first generated -- no trees, plants, lakes, snow, ice, ores or structures. One class, two
 * registrations, told apart by the {@link ChunkKind} each restores -- the Mountain works on land chunks, the River on
 * water chunks, and each refuses when the carrier's own chunk is the other kind.
 *
 * <p>Extends {@link AsBeforeGuItem}. The gate reads the carrier's chunk alone: loaded and of this Gu's kind; the
 * payout then hands every loaded chunk of the 3×3 square that is of this kind to
 * {@code ChunkRewindService.rewindAround}, which generates the square again from the seed and restores one chunk
 * per server tick. A neighbor of the other kind is skipped in silence. Where {@code ModEvents.canGrief} keeps the
 * carrier from changing the terrain ({@code mobGriefing}), the Gu refuses before anything is spent.
 *
 * @author Alex
 * @version 1.0.0
 * @see AsBeforeGuItem
 * @see ChunkRewindService
 * @since 1.0.0
 */

public class TerrainAsBeforeGuItem extends AsBeforeGuItem {

    private static final String FAILED_NO_GRIEF = "guzhenren.item.failed.as_before_no_grief";
    private static final String FAILED_UNKNOWN_LAND = "guzhenren.item.failed.as_before_unknown_land";
    private static final String FAILED_NOT_LAND = "guzhenren.item.failed.as_before_not_land";
    private static final String FAILED_NOT_WATER = "guzhenren.item.failed.as_before_not_water";
    private static final String MSG_LAND = "guzhenren.item.gu.as_before_land";
    private static final String MSG_WATER = "guzhenren.item.gu.as_before_water";
    private final ChunkKind kind;

    public TerrainAsBeforeGuItem(Properties properties, ChunkKind kind, GuSpec spec) {
        super(properties, spec);
        this.kind = kind;
    }

    public ChunkKind getKind() { return kind; }

    @Override
    protected @Nullable Refusal checkPayoutGate(Player player, ItemStack stack) {
        Refusal recovering = checkRecoverGate(player, stack);
        if (recovering != null) return recovering;
        if (!(player instanceof ServerPlayer server)) return null;

        ServerLevel level = server.serverLevel();
        if (!ModEvents.canGrief(level, server)) return new Refusal(FAILED_NO_GRIEF);
        LevelChunk center = getLoaded(level, server.chunkPosition());
        if (center == null) return new Refusal(FAILED_UNKNOWN_LAND);
        if (ChunkKind.of(level, center) != kind) {
            return new Refusal(kind == ChunkKind.LAND ? FAILED_NOT_LAND : FAILED_NOT_WATER);
        }
        return null;
    }

    @Override
    protected void payOut(ServerPlayer player, ItemStack stack) {
        ServerLevel level = player.serverLevel();
        ChunkPos center = player.chunkPosition();
        int radius = ChunkRewindService.RESTORE_RADIUS;
        List<ChunkPos> targets = new ArrayList<>();
        for (int x = center.x - radius; x <= center.x + radius; x++) {
            for (int z = center.z - radius; z <= center.z + radius; z++) {
                LevelChunk chunk = getLoaded(level, new ChunkPos(x, z));
                if (chunk != null && ChunkKind.of(level, chunk) == kind) targets.add(chunk.getPos());
            }
        }
        ChunkRewindService.rewindAround(level, center, targets);
        ActionBar.inform(player, kind == ChunkKind.LAND ? MSG_LAND : MSG_WATER);
    }

    private static @Nullable LevelChunk getLoaded(ServerLevel level, ChunkPos pos) {
        return level.getChunkSource().getChunkNow(pos.x, pos.z);
    }
}
