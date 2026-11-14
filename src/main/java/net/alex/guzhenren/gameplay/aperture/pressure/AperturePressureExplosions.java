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
 * 一维度内在挖的空窍[Aperture]压力爆炸坑，存该维度 {@code guzhenren_pressure_explosions.dat}。
 * <ul>
 *   <li>文件所在维度即爆炸维度；各爆炸自写状态（{@link AperturePressureExplosionTask}）。</li>
 *   <li>维度文件于首次请求的 tick 加载：恢复的爆炸开服一 tick、区块重载后即续。</li>
 *   <li>存储工厂只建一次、无维度：磁盘读出的条目在该次 {@link #get} 返回前才绑定维度。</li>
 *   <li>⚠ 无静态列表：爆炸随维度存亡，切世界无过期引用；停服存档晚于 {@code ServerStoppingEvent}，仍见全部爆炸。</li>
 *   <li>⚠ 文件随边缘列表变大：半径 112 至多约 55 万边缘方块，gzip 前约 4.4 MB，仅进行中写。</li>
 *   <li>{@link #tickAll}：全维度爆炸每 tick 共用 {@link #TICK_BUDGET_NANOS}（50 ms 中 10 ms），每
 *       {@link #CLOCK_CHECK_INTERVAL} 个位置读一次钟；维度依服务器序、爆炸依启动序，先启先完。</li>
 *   <li>预算为参数，便于 GameTest 定量推进：零预算首次读钟前仍跑 {@code CLOCK_CHECK_INTERVAL - 1} 个位置。</li>
 *   <li>有爆炸运行的 tick 都标脏，含最后挖完那 tick：下次存档写空列表，挖完的不复活。</li>
 * </ul>
 *
 * @author Alex
 * @version 1.0.0
 * @see AperturePressureEvents
 * @since 1.0.0
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
