package net.alex.guzhenren.gameplay.path.strength;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import net.alex.guzhenren.Guzhenren;
import net.alex.guzhenren.core.ModEvents;
import net.alex.guzhenren.gameplay.attribute.StrengthService;
import net.alex.guzhenren.gameplay.attribute.StrengthSnapshot;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * The attack round [攻击轮次] of the beast strength phantom path [兽力虚影流] and the {@link StrengthService.ReleaseSource}
 * it answers. A fist hit opens a round of {@link #ROUND_TICKS} when the last one began at least that long ago
 * ({@link BeastPhantomEvents} gates the hit); every hit inside it shares what the opening drew, and no hit re-rolls.
 *
 * <p>Only the families of the permanent beast strengths are drawn; a timed strength alone (the Brute Force Longhorn
 * Beetle's ox) opens nothing. The opening hit rolls once against
 * {@link #getChance}: {@link #CHANCE} plus the held beasts' {@link BeastStrength#getChanceBonus} (the brown bear's
 * innate strength makes it 10%), or 80/90/100% while the rank three/four/five All-Out Effort Gu [全力以赴] runs
 * ({@code StrengthSnapshot.allOutTier}). It draws {@link #getDrawCount}: under Hardship Strength [苦力] the streak's
 * count of distinct families, else one when the roll lands, else none -- the round still runs, so the next hits do not
 * roll again. The streak counts rounds opened within {@link #STREAK_WINDOW_TICKS} of the last one's end while Hardship
 * runs, and goes back to one past that window or when Hardship stops.
 *
 * <p>While the round runs it fully releases [全面解放] every drawn family while All-Out Effort runs NOW, else the first
 * one when the opening roll landed, through one {@link StrengthService#getReleasedExtra} over their summed permanent
 * points, still cut by the current capacity; any other drawn family is only shown ({@link #getFullyReleased} names
 * the released ones, which also lend their beasts' speed to {@code SprintSpeedService}). The drawn set is fixed at
 * the opening; a family the body no longer holds leaves it, and its phantom goes, the next tick. Outside a round the
 * release is zero.
 *
 * <p>The Strength Qi Gu's [力气蛊] ranged punch keeps to the rounds ({@link #canRange}): at most one a round, at
 * least {@link #ROUND_TICKS} apart. One thrown outside a round opens one as a fist would ({@link #startRange}); one
 * thrown by a body without a permanent beast opens none and keeps only the spacing.
 *
 * <p>⚠ Server memory only, keyed by player: never saved, cleared on death, logout, dimension change and server stop.
 * A client asks nothing of it and reads no release. ⚠ {@link #CHANCE}, {@link #ROUND_TICKS} and
 * {@link #STREAK_WINDOW_TICKS} are provisional. The int and list overloads are seams for unit tests.
 *
 * @author Alex
 * @version 1.0.0
 * @see BeastPhantomEvents
 * @see BeastPhantomsDrawnEvent
 * @see HardshipStrength
 * @since 1.0.0
 */

@EventBusSubscriber(modid = Guzhenren.MOD_ID)
public final class StrengthRound {

    private StrengthRound() {}

    public static final float CHANCE = 0.05F;
    private static final float[] ALL_OUT_CHANCES = { 0.80F, 0.90F, 1.0F };
    public static final int ROUND_TICKS = 20;
    public static final int STREAK_WINDOW_TICKS = 20;
    private static final Map<UUID, State> ROUNDS = new HashMap<>();
    private static final Map<UUID, Long> RANGED = new HashMap<>();

    private static final class State {

        private final long start;
        private final List<BeastStrengthFamily> families;
        private final @Nullable BeastStrengthFamily rolledFull;
        private int streak;

        private State(long start, List<BeastStrengthFamily> families, @Nullable BeastStrengthFamily rolledFull,
                      int streak) {
            this.start = start;
            this.families = families;
            this.rolledFull = rolledFull;
            this.streak = streak;
        }
    }

    //region Round [轮次]
    public static float open(@NotNull ServerPlayer player, float roll) {
        long now = player.level().getGameTime();
        State last = ROUNDS.get(player.getUUID());
        if (last != null && !isOver(last.start, now)) return 0.0F;

        PathStrengthData data = PathStrengthService.get(player);
        List<BeastStrengthFamily> available = getAvailableFamilies(data);
        if (available.isEmpty()) return 0.0F;

        boolean hardship = HardshipStrength.isActive(player);
        boolean rolled = roll < getChance(StrengthService.getAllOutTier(player), data.getBeastChanceBonus());
        int streak = hardship ? getNextStreak(last == null ? 0 : last.streak, last == null ? now : last.start, now) : 0;
        int count = getDrawCount(hardship, rolled, streak, available.size());
        List<BeastStrengthFamily> families = pickFamilies(available, count, player.getRandom());
        BeastStrengthFamily rolledFull = rolled && !families.isEmpty() ? families.getFirst() : null;
        ROUNDS.put(player.getUUID(), new State(now, families, rolledFull, streak));
        ModEvents.post(new BeastPhantomsDrawnEvent(player, families));
        return (float) (getReleasedExtra(player) * PathStrengthData.ATTACK_PER_POINT);
    }

    public static boolean isRunning(@NotNull Player player) {
        State state = ROUNDS.get(player.getUUID());
        return player instanceof ServerPlayer && state != null && !isOver(state.start, player.level().getGameTime());
    }

    public static @NotNull List<BeastStrengthFamily> getFamilies(@NotNull Player player) {
        State state = ROUNDS.get(player.getUUID());
        return isRunning(player) && state != null ? List.copyOf(state.families) : List.of();
    }

    public static void clear(@NotNull Player player) {
        ROUNDS.remove(player.getUUID());
        RANGED.remove(player.getUUID());
    }

    public static boolean canRange(@NotNull ServerPlayer player) {
        long now = player.level().getGameTime();
        State state = ROUNDS.get(player.getUUID());
        Long roundStart = state != null && !isOver(state.start, now) ? state.start : null;
        return canRange(RANGED.get(player.getUUID()), roundStart, now);
    }

    public static boolean canRange(@Nullable Long lastRanged, @Nullable Long roundStart, long now) {
        if (lastRanged == null) return true;
        if (now - lastRanged < ROUND_TICKS) return false;
        return roundStart == null || lastRanged < roundStart;
    }

    public static float startRange(@NotNull ServerPlayer player, float roll) {
        float gain = isRunning(player) ? 0.0F : open(player, roll);
        RANGED.put(player.getUUID(), player.level().getGameTime());
        return gain;
    }

    public static boolean hasRangedThisRound(@NotNull ServerPlayer player) {
        State state = ROUNDS.get(player.getUUID());
        Long ranged = RANGED.get(player.getUUID());
        return isRunning(player) && state != null && ranged != null && ranged >= state.start;
    }

    public static boolean isOver(long start, long now) { return now - start >= ROUND_TICKS; }

    public static int getNextStreak(int lastStreak, long lastStart, long now) {
        boolean chained = lastStreak > 0 && now - (lastStart + ROUND_TICKS) <= STREAK_WINDOW_TICKS;
        return chained ? lastStreak + 1 : 1;
    }

    public static float getChance(int allOutTier, float beastBonus) {
        if (allOutTier < StrengthService.ALL_OUT_LOWEST_TIER) return CHANCE + beastBonus;
        return ALL_OUT_CHANCES[Math.min(allOutTier - StrengthService.ALL_OUT_LOWEST_TIER, ALL_OUT_CHANCES.length - 1)];
    }

    public static int getDrawCount(boolean hardship, boolean rolled, int streak, int families) {
        if (hardship) return Math.min(Math.max(1, streak), families);
        return rolled ? Math.min(1, families) : 0;
    }

    public static @NotNull List<BeastStrengthFamily> getAvailableFamilies(@NotNull PathStrengthData data) {
        List<BeastStrengthFamily> families = new ArrayList<>();
        for (BeastStrengthFamily family : BeastStrengthFamily.values()) {
            if (data.getBeastPoints(family) > 0) families.add(family);
        }
        return families;
    }

    public static @NotNull List<BeastStrengthFamily> pickFamilies(@NotNull List<BeastStrengthFamily> available,
                                                                  int count, @NotNull RandomSource random) {
        List<BeastStrengthFamily> pool = new ArrayList<>(available);
        List<BeastStrengthFamily> picked = new ArrayList<>();
        while (picked.size() < count && !pool.isEmpty()) picked.add(pool.remove(random.nextInt(pool.size())));
        return picked;
    }
    //endregion

    //region Release [解放]
    public static int getReleasedExtra(@NotNull ServerPlayer player) {
        return getReleasedExtra(player, StrengthService.snapshot(player));
    }

    private static int getReleasedExtra(@NotNull Player player, @NotNull StrengthSnapshot snapshot) {
        State state = ROUNDS.get(player.getUUID());
        if (!isRunning(player) || state == null || state.families.isEmpty()) return 0;

        return getReleasedExtra(snapshot.capacity(), snapshot.totalPoints(), state.families,
                PathStrengthService.get(player), snapshot.allOut(), state.rolledFull);
    }

    public static int getReleasedExtra(int capacity, int total, @NotNull List<BeastStrengthFamily> families,
                                       @NotNull PathStrengthData permanent, boolean allOut,
                                       @Nullable BeastStrengthFamily rolledFull) {
        int fullPoints = 0;
        for (BeastStrengthFamily family : getFullyReleased(families, allOut, rolledFull)) {
            fullPoints += Math.max(0, permanent.getBeastPoints(family));
        }
        return StrengthService.getReleasedExtra(capacity, total, fullPoints);
    }

    public static @NotNull List<BeastStrengthFamily> getFullyReleased(@NotNull Player player) {
        State state = ROUNDS.get(player.getUUID());
        if (!isRunning(player) || state == null) return List.of();
        return getFullyReleased(state.families, StrengthService.isAllOut(player), state.rolledFull);
    }

    public static @NotNull List<BeastStrengthFamily> getFullyReleased(@NotNull List<BeastStrengthFamily> families,
                                                                      boolean allOut,
                                                                      @Nullable BeastStrengthFamily rolledFull) {
        if (allOut) return List.copyOf(families);
        return rolledFull != null && families.contains(rolledFull) ? List.of(rolledFull) : List.of();
    }
    //endregion

    //region Event [事件]
    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;

        State state = ROUNDS.get(player.getUUID());
        if (state == null) return;

        long now = player.level().getGameTime();
        if (now - (state.start + ROUND_TICKS) > STREAK_WINDOW_TICKS) {
            ROUNDS.remove(player.getUUID());
            return;
        }
        if (!HardshipStrength.isActive(player)) state.streak = 0;
        if (isOver(state.start, now)) return;

        PathStrengthData data = PathStrengthService.get(player);
        if (state.families.removeIf(family -> data.getBeastPoints(family) <= 0)) {
            ModEvents.post(new BeastPhantomsDrawnEvent(player, state.families));
        }
    }

    @SubscribeEvent
    public static void onDeath(LivingDeathEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) clear(player);
    }

    @SubscribeEvent
    public static void onLoggedOut(PlayerEvent.PlayerLoggedOutEvent event) { clear(event.getEntity()); }

    @SubscribeEvent
    public static void onChangedDimension(PlayerEvent.PlayerChangedDimensionEvent event) { clear(event.getEntity()); }

    @SubscribeEvent
    public static void onServerStopped(ServerStoppedEvent event) {
        ROUNDS.clear();
        RANGED.clear();
    }

    @SubscribeEvent
    public static void onCommonSetup(FMLCommonSetupEvent event) {
        StrengthService.setReleaseSource((player, snapshot) ->
                player instanceof ServerPlayer ? getReleasedExtra(player, snapshot) : 0);
    }
    //endregion
}
