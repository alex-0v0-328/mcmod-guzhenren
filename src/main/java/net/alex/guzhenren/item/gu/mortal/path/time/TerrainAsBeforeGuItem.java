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
 * 山如故[Mountain As Before Gu]与江如故[River As Before Gu]物品：继承 {@link AsBeforeGuItem}，将周围区块回溯为世界种子
 * 初次生成的裸地形。
 * <ul>
 *   <li>裸地形指仅保留基础地形与地层，不含树木、植被、湖泊、冰雪、矿脉与结构；两只蛊共用本类，按目标 {@link ChunkKind}
 *       区分：山如故仅作用于陆地区块，江如故仅作用于水域区块，玩家脚下所在中心区块属于另一类型时直接拒绝。</li>
 *   <li>催动生效时收集以玩家所在区块为中心 5×5 范围内已加载的同类型区块，交由 {@link ChunkRewindService#rewindAround}
 *       按世界种子重新生成并每服务端 tick 恢复一个区块（范围内的异类邻块静默跳过）；若 {@link ModEvents#canGrief} 禁止
 *       破坏地形（如关闭 {@code mobGriefing}），则在扣费前拒绝。</li>
 * </ul>
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
