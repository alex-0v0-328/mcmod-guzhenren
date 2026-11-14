package net.alex.guzhenren.item.gu.mortal.path.time;

import net.alex.guzhenren.core.ModEvents;
import java.util.ArrayList;
import java.util.List;
import net.alex.guzhenren.gameplay.rewind.ChunkKind;
import net.alex.guzhenren.gameplay.rewind.ChunkRewindService;
import net.alex.guzhenren.item.gu.GuProperties;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.chunk.LevelChunk;
import org.jetbrains.annotations.Nullable;

/**
 * 山如故[Mountain As Before Gu]与江如故[River As Before Gu]：周围区块回到初次生成的裸地形。
 * <ul>
 *   <li>裸地形即无树木、植物、湖泊、雪、冰、矿石、结构；一类两注册，按恢复的 {@link ChunkKind} 区分：山如故管陆地区块，
 *       江如故管水域区块，所在区块为另一种时拒绝。</li>
 *   <li>结算把 5×5 内已加载的同种区块交 {@code ChunkRewindService.rewindAround}：按种子重生成，每服务端 tick 恢复一块；
 *       异种邻块静默跳过；{@code ModEvents.canGrief} 不许改地形（{@code mobGriefing}）时扣费前拒绝。</li>
 * </ul>
 *
 * @author Alex
 * @version 1.0.0
 * @see AsBeforeGuItem
 * @see ChunkRewindService
 * @since 1.0.0
 */

public class TerrainAsBeforeGuItem extends AsBeforeGuItem {

    private static final String FAILED_NO_GRIEF = "guzhenren.item.failed.as_before_no_grief";
    private final ChunkKind kind;

    public TerrainAsBeforeGuItem(ChunkKind kind, GuProperties guProperties) {
        super(guProperties);
        this.kind = kind;
    }

    @Override
    protected @Nullable Refusal checkEffect(Player player, ItemStack stack) {
        if (!(player instanceof ServerPlayer server)) return null;

        ServerLevel level = server.serverLevel();
        if (!ModEvents.canGrief(level, server)) return new Refusal(FAILED_NO_GRIEF);
        LevelChunk center = getLoaded(level, server.chunkPosition());
        if (center == null) return new Refusal(Refusal.CANNOT_USE);
        if (ChunkKind.of(level, center) != kind) {
            return new Refusal(Refusal.WRONG_TARGET);
        }
        return null;
    }

    @Override
    protected void applyEffect(ServerPlayer player, ItemStack stack) {
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
    }

    private static @Nullable LevelChunk getLoaded(ServerLevel level, ChunkPos pos) {
        return level.getChunkSource().getChunkNow(pos.x, pos.z);
    }
}
