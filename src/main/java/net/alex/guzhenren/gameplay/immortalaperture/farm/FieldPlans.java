package net.alex.guzhenren.gameplay.immortalaperture.farm;

import java.util.function.Function;
import java.util.function.Supplier;
import net.alex.guzhenren.core.ModEvents;
import net.alex.guzhenren.gameplay.immortalaperture.plot.Siting;
import net.alex.guzhenren.gameplay.immortalaperture.state.BlissfulLandData;
import net.alex.guzhenren.gameplay.immortalaperture.state.LandNoticeEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.ChunkPos;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * 标准田的规划队列：主人在地图上点选区块入队，地灵按队列每秒开垦 1 块（不设冷却）。
 * <ul>
 *   <li>{@link #plan}：按 {@link Siting#field} 复判（服务端经 {@link Siting#loadedTiles} 同步加载目标区块现扫地图片），
 *       过了才入队；{@link #unplan} 直接出队。</li>
 *   <li>{@link #tick} 由 {@code BlissfulLands} 每秒调用：队列非空、主人在线时取队首，用真实方块再按同一规则复判（排除
 *       它自己这一格规划），过了就 {@link FarmWork#reclaim}，否则取消这一格并提示主人；主人不在线整条队列原样保留。一次
 *       只开 1 块：整平一个区块是这里最重的活，按秒摊开防卡服。</li>
 *   <li>提示发 {@link LandNoticeEvent}：主人开着地灵面板就走面板顶部提示；没开面板时只有取消才发聊天消息，开垦成功不
 *       打扰。</li>
 *   <li>{@link #step} 接受「区块 → 田」的布局函数；GameTest 把它换成测试区里的小田，规则不变。</li>
 * </ul>
 */

public final class FieldPlans {

    private FieldPlans() {}

    public record Step(boolean reclaimed, @NotNull Component notice) {}

    public static @NotNull Siting.Reason plan(@NotNull ServerLevel level, @NotNull BlissfulLandData data,
                                              @NotNull Siting.Plot plot, int chunkX, int chunkZ) {
        Siting.Reason reason = Siting.field(plot, Siting.loadedTiles(level, data.territory()), chunkX, chunkZ);
        if (reason == Siting.Reason.OK) FarmState.of(data).addPlan(ChunkPos.asLong(chunkX, chunkZ));
        return reason;
    }

    public static boolean unplan(@NotNull BlissfulLandData data, int chunkX, int chunkZ) {
        return FarmState.of(data).removePlan(ChunkPos.asLong(chunkX, chunkZ));
    }

    public static void tick(@NotNull ServerLevel level, @NotNull BlissfulLandData data,
                            @NotNull Supplier<Siting.Plot> plot) {
        if (FarmState.of(data).plans().isEmpty() || data.owner() == null) return;
        ServerPlayer owner = level.getServer().getPlayerList().getPlayer(data.owner());
        if (owner == null) return;

        Step step = step(level, data, owner, chunk -> FieldPlot.of(chunk.x, chunk.z, 0), plot);
        if (step == null) return;
        ModEvents.post(new LandNoticeEvent(owner, step.notice(), !step.reclaimed()));
    }

    public static @Nullable Step step(@NotNull ServerLevel level, @NotNull BlissfulLandData data,
                                           @NotNull ServerPlayer owner, @NotNull Function<ChunkPos, FieldPlot> layout,
                                           @NotNull Supplier<Siting.Plot> plot) {
        if (FarmState.of(data).plans().isEmpty()) return null;
        ChunkPos chunk = new ChunkPos(FarmState.of(data).plans().getLong(0));
        FarmState.of(data).removePlan(chunk.toLong());
        FieldPlot field = layout.apply(chunk);
        Siting.Reason reason = Siting.place(plot.get(), chunk.x, chunk.z, 1);
        if (reason == Siting.Reason.OK) {
            reason = Siting.columns(Siting.loadedTiles(level, data.territory()), field.minX(), field.minZ(),
                    field.maxX(), field.maxZ(), Siting.FIELD_SLOPE, false, true);
        }
        if (reason != Siting.Reason.OK) {
            return new Step(false, LandNoticeEvent.text("plan_cancelled", Component.translatable(reason.key())));
        }
        if (FarmWork.reclaim(level, data, field, owner) == null) {
            return new Step(false, LandNoticeEvent.text("plan_cancelled", LandNoticeEvent.text("grief_denied")));
        }
        return new Step(true, LandNoticeEvent.text("reclaimed", FarmState.of(data).fields().size()));
    }
}
