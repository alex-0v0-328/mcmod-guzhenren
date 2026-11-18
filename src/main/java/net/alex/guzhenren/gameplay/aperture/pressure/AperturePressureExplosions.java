package net.alex.guzhenren.gameplay.aperture.pressure;

import java.util.ArrayList;
import java.util.List;
import net.alex.guzhenren.core.ModSavedData;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;
import org.jetbrains.annotations.NotNull;

/**
 * 单维度内正在挖掘的空窍压力爆炸任务集合，持久化于该维度的 {@code guzhenren_pressure_explosions.dat}。
 * <ul>
 *   <li>存档文件所在维度即爆炸所属维度；每场爆炸的具体阶段与游标由 {@link AperturePressureExplosionTask} 读写。</li>
 *   <li>{@link SavedData.Factory} 构造时不带维度上下文，反序列化先暂存原始列表，在首次调用 {@link #get} 时才绑定
 *       {@link ServerLevel} 并还原任务，因此重启后首个服务端 tick 即可无缝续挖。</li>
 *   <li>⚠ 不设全局静态列表，所有任务随维度 {@link SavedData} 生命周期存亡，单人切换存档不会残留过期维度引用；停服保存晚
 *       于 {@code ServerStoppingEvent}，进行中的任务会自动落盘。</li>
 *   <li>⚠ 存档体积随边缘列表（{@code rim}）增长：最大半径 112 时至多约 55 万个边缘坐标（压缩前约 4.4 MB），仅在挖掘未完
 *       成期间写入磁盘。</li>
 *   <li>{@link #tickAll} 让所有维度的活动爆炸共享每 tick {@link #TICK_BUDGET_NANOS}（10 ms）时间预算，每处理
 *       {@link #CLOCK_CHECK_INTERVAL} 个位置读一次纳秒时钟；按维度遍历顺序与任务启动先后依次推进。传入 {@code 0} 预算时
 *       在首次查钟前仍会推进 {@code CLOCK_CHECK_INTERVAL - 1} 个位置，供 GameTest 定量步进。</li>
 *   <li>⚠ 只要本 tick 有任务执行（含全部挖完清空的那一 tick）就必须调用 {@code setDirty()}，确保下次保存写入空列表，防
 *       止已挖完的大坑在重启后复发。</li>
 * </ul>
 */

public final class AperturePressureExplosions extends ModSavedData {

    private AperturePressureExplosions() {}

    public static final String NAME = "guzhenren_pressure_explosions";
    private static final long TICK_BUDGET_NANOS = 10_000_000L;
    private static final int CLOCK_CHECK_INTERVAL = 256;
    private static final SavedData.Factory<AperturePressureExplosions> FACTORY =
            new SavedData.Factory<>(AperturePressureExplosions::new, (tag, registries) -> unbound(tag));
    private final List<AperturePressureExplosionTask> tasks = new ArrayList<>();
    private ListTag unbound;

    public static @NotNull AperturePressureExplosions get(@NotNull ServerLevel level) {
        AperturePressureExplosions explosions = ModSavedData.get(level, NAME, FACTORY);
        explosions.bind(level);
        return explosions;
    }

    public static void tickAll(@NotNull MinecraftServer server) { tickAll(server, TICK_BUDGET_NANOS); }

    public static void tickAll(@NotNull MinecraftServer server, long budgetNanos) {
        TickBudget budget = new TickBudget(System.nanoTime() + budgetNanos);
        for (ServerLevel level : server.getAllLevels()) get(level).tick(budget);
    }

    public static @NotNull AperturePressureExplosions load(@NotNull ServerLevel level, @NotNull CompoundTag tag) {
        AperturePressureExplosions explosions = unbound(tag);
        explosions.bind(level);
        return explosions;
    }

    private static AperturePressureExplosions unbound(CompoundTag tag) {
        AperturePressureExplosions explosions = new AperturePressureExplosions();
        explosions.unbound = tag.getList("explosions", Tag.TAG_COMPOUND);
        return explosions;
    }

    private void bind(ServerLevel level) {
        ListTag entries = unbound;
        if (entries == null) return;

        unbound = null;
        for (int i = 0; i < entries.size(); i++) {
            AperturePressureExplosionTask task = AperturePressureExplosionTask.load(level, entries.getCompound(i));
            if (task != null) tasks.add(task);
        }
    }

    public boolean isEmpty() { return tasks.isEmpty(); }

    @Override
    protected void write(@NotNull CompoundTag tag, @NotNull StackIo stacks) {
        ListTag entries = new ListTag();
        for (AperturePressureExplosionTask task : tasks) entries.add(task.save());
        tag.put("explosions", entries);
    }

    void add(AperturePressureExplosionTask task) {
        tasks.add(task);
        setDirty();
    }

    private void tick(TickBudget budget) {
        if (tasks.isEmpty()) return;

        tasks.removeIf(task -> task.tick(budget));
        setDirty();
    }

    static final class TickBudget {

        private final long deadline;
        private int sinceClockCheck;
        private boolean spent;

        private TickBudget(long deadline) { this.deadline = deadline; }

        boolean hasTime() {
            if (spent) return false;
            if (++sinceClockCheck < CLOCK_CHECK_INTERVAL) return true;
            sinceClockCheck = 0;
            spent = System.nanoTime() >= deadline;
            return !spent;
        }
    }
}
