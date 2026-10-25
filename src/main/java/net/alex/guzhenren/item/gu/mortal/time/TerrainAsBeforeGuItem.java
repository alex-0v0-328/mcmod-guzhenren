package net.alex.guzhenren.item.gu.mortal.time;

import net.alex.guzhenren.core.ActionBar;
import net.alex.guzhenren.core.ModEvents;
import net.alex.guzhenren.gameplay.rewind.ChunkKind;
import net.alex.guzhenren.gameplay.rewind.ChunkMemory;
import net.alex.guzhenren.gameplay.rewind.ChunkRewindService;
import net.alex.guzhenren.gameplay.rewind.ChunkSnapshot;
import net.alex.guzhenren.item.gu.GuSpec;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.chunk.LevelChunk;
import org.jetbrains.annotations.Nullable;

/**
 * Mountain As Before Gu [山如故] and River As Before Gu [江如故]: the chunks around the carrier go back to how they
 * stood a day ago. One class, two registrations, told apart by the {@link ChunkKind} each restores -- the Mountain
 * works on land chunks, the River on water chunks, and each refuses when the carrier's own chunk is the other kind.
 *
 * <p>Extends {@link AsBeforeGuItem}. The gate reads the carrier's chunk alone: loaded, of this Gu's kind, with a
 * {@code ChunkMemory} that holds a snapshot a day old; the payout then queues every chunk of the 3×3 square that is
 * loaded, of this kind and remembered ({@code ChunkRewindService.enqueue}), one chunk restored per server tick. A
 * neighbor without a memory is skipped in silence; the center without one refuses before anything is spent.
 * Where {@code ModEvents.canGrief} keeps the carrier from changing the terrain ({@code mobGriefing}), the Gu
 * refuses before anything is spent.
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
        ChunkMemory memory = ChunkRewindService.getMemory(center);
        if (memory.isEmpty()) return new Refusal(FAILED_UNKNOWN_LAND);
        return memory.pick(level.getGameTime()) == null ? new Refusal(FAILED_TOO_YOUNG) : null;
    }

    @Override
    protected void payOut(ServerPlayer player, ItemStack stack) {
        ServerLevel level = player.serverLevel();
        long now = level.getGameTime();
        ChunkPos center = player.chunkPosition();
        int radius = ChunkRewindService.RESTORE_RADIUS;
        for (int x = center.x - radius; x <= center.x + radius; x++) {
            for (int z = center.z - radius; z <= center.z + radius; z++) {
                LevelChunk chunk = getLoaded(level, new ChunkPos(x, z));
                if (chunk == null || ChunkKind.of(level, chunk) != kind) continue;

                ChunkSnapshot snapshot = ChunkRewindService.pick(chunk, now);
                if (snapshot != null) ChunkRewindService.enqueue(level, chunk.getPos(), snapshot);
            }
        }
        ActionBar.inform(player, kind == ChunkKind.LAND ? MSG_LAND : MSG_WATER);
    }

    private static @Nullable LevelChunk getLoaded(ServerLevel level, ChunkPos pos) {
        return level.getChunkSource().getChunkNow(pos.x, pos.z);
    }
}
