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
 * The pressure-explosion craters [空窍压力爆炸] still being carved in one level, kept in that level's data storage as
 * {@code guzhenren_pressure_explosions.dat}, so a server stop mid-blast resumes the crater on the next start instead
 * of leaving it half-carved. The level that owns the file is the blasts' dimension; each blast writes its own state
 * ({@link AperturePressureExplosionTask}). A level's file loads on the first server tick that asks for it, so a
 * restored blast carries on one tick after the server starts, as soon as its chunks are loaded again. The storage
 * factory is built once and cannot hold the level, so a file read from disk keeps its entries unbound for the rest
 * of the {@link #get} call that read it, which binds them to the level before it returns.
 *
 * <p>{@link #tickAll} runs the blasts of every level against one budget a tick, {@link #TICK_BUDGET_NANOS} (10 of
 * the tick's 50 ms), the clock read every {@link #CLOCK_CHECK_INTERVAL} positions; levels go in the server's order
 * and blasts in start order, so the older blast finishes first. The budget is an argument so a GameTest can step a
 * blast by a fixed amount: a zero budget still runs {@code CLOCK_CHECK_INTERVAL - 1} positions before its first
 * clock read. A level's data turns dirty on every tick one of its blasts runs, the tick the last one finishes
 * included, so the next save writes the empty list and a finished blast never comes back.
 *
 * <p>⚠ No static task list: the blasts live and die with their level, so a singleplayer world switch never carries
 * a stale level reference, and the stop save -- which runs after {@code ServerStoppingEvent} -- still sees every
 * running blast. ⚠ The file size follows the rim list: radius 112 clears up to ≈0.55 million rim blocks, ≈4.4 MB
 * before gzip, written only while that blast runs.
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
