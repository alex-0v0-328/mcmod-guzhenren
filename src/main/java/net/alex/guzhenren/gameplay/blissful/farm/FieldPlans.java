package net.alex.guzhenren.gameplay.blissful.farm;

import java.util.function.Function;
import net.alex.guzhenren.gameplay.blissful.land.BlissfulLandData;
import net.alex.guzhenren.gameplay.blissful.map.MapModule;
import net.alex.guzhenren.gameplay.blissful.map.Siting;
import net.alex.guzhenren.gameplay.blissful.panel.EarthSpiritMenu;
import net.alex.guzhenren.gameplay.blissful.panel.EarthSpiritPanel;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.ChunkPos;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * 标准田的规划队列：主人在地图上点选区块入队，地灵按队列每 {@value FarmWork#RECLAIM_COOLDOWN_TICKS} 刻开垦 1 块。
 * <ul>
 *   <li>{@link #plan}：按 {@link Siting#field} 复判（服务端经 {@link MapModule#tiles} 同步加载目标区块现扫地图片），
 *       过了才入队；{@link #unplan} 直接出队。</li>
 *   <li>{@link #tick} 由 {@code BlissfulLands} 每秒调用：队列非空、冷却已过、主人在线时取队首，用真实方块再按同一规则
 *       复判（排除它自己这一格规划），过了就 {@link FarmWork#reclaim}，否则取消这一格并提示主人；主人不在线整条队列原
 *       样保留。队首不等冷却：冷却只在开垦后才开始算。</li>
 *   <li>提示：主人开着地灵面板就走面板顶部提示；没开面板时只有取消才发聊天消息，开垦成功不打扰。</li>
 *   <li>{@link #step} 接受「区块 → 田」的布局函数；GameTest 把它换成测试区里的小田，规则不变。</li>
 * </ul>
 */

public final class FieldPlans {

    private FieldPlans() {}

    public record Step(boolean reclaimed, @NotNull Component notice) {}

    public static @NotNull Siting.Reason plan(@NotNull ServerLevel level, @NotNull BlissfulLandData data, int chunkX,
                                              int chunkZ) {
        Siting.Reason reason = Siting.field(MapModule.plot(level, data), MapModule.tiles(level, data.territory()),
                chunkX, chunkZ);
        if (reason == Siting.Reason.OK) data.farm().addPlan(ChunkPos.asLong(chunkX, chunkZ));
        return reason;
    }

    public static boolean unplan(@NotNull BlissfulLandData data, int chunkX, int chunkZ) {
        return data.farm().removePlan(ChunkPos.asLong(chunkX, chunkZ));
    }

    public static void tick(@NotNull ServerLevel level, @NotNull BlissfulLandData data) {
        if (data.farm().plans().isEmpty() || FarmWork.reclaimCooldown(level, data) > 0 || data.owner() == null) return;
        ServerPlayer owner = level.getServer().getPlayerList().getPlayer(data.owner());
        if (owner == null) return;

        Step step = step(level, data, owner, chunk -> FieldPlot.of(chunk.x, chunk.z, 0));
        if (step == null) return;
        if (owner.containerMenu instanceof EarthSpiritMenu) EarthSpiritPanel.refresh(owner, step.notice());
        else if (!step.reclaimed()) owner.sendSystemMessage(step.notice());
    }

    public static @Nullable Step step(@NotNull ServerLevel level, @NotNull BlissfulLandData data,
                                           @NotNull ServerPlayer owner, @NotNull Function<ChunkPos, FieldPlot> layout) {
        if (data.farm().plans().isEmpty()) return null;
        ChunkPos chunk = new ChunkPos(data.farm().plans().getLong(0));
        data.farm().removePlan(chunk.toLong());
        FieldPlot plot = layout.apply(chunk);
        Siting.Reason reason = Siting.place(MapModule.plot(level, data), chunk.x, chunk.z, 1);
        if (reason == Siting.Reason.OK) {
            reason = Siting.columns(MapModule.tiles(level, data.territory()), plot.minX(), plot.minZ(), plot.maxX(),
                    plot.maxZ(), Siting.FIELD_SLOPE, false, true);
        }
        if (reason != Siting.Reason.OK) {
            return new Step(false, EarthSpiritPanel.notice("plan_cancelled", chunk.x, chunk.z,
                    Component.translatable(reason.key())));
        }
        if (FarmWork.reclaim(level, data, plot, owner) == null) {
            return new Step(false, EarthSpiritPanel.notice("plan_cancelled", chunk.x, chunk.z,
                    EarthSpiritPanel.notice("grief_denied")));
        }
        return new Step(true, EarthSpiritPanel.notice("reclaimed", chunk.x, chunk.z, data.farm().fields().size()));
    }
}
