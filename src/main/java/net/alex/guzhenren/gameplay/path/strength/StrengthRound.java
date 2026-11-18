package net.alex.guzhenren.gameplay.path.strength;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import net.alex.guzhenren.Guzhenren;
import net.alex.guzhenren.core.ModEvents;
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
 * 兽力虚影流[Beast Strength Phantom]的攻击轮次与全面解放服务（在通用初始化时注册为
 * {@link StrengthService.ReleaseSource}）。
 * <ul>
 *   <li>当空手命中通过 {@link BeastPhantomEvents} 门槛且距上一轮开始已满 {@link #ROUND_TICKS}（1 秒）时开启新轮次
 *       （仅从永久兽力[Beast Strength]兽种中抽取，仅有蛮力天牛蛊等限时兽力时不开轮）；开轮当击按 {@link #getChance} 掷
 *       全面解放骰（基础 {@link #CHANCE} + 永久兽力概率加成，三/四/五转全力以赴蛊[All-Out Effort Gu]下直接提升至 80% /
 *       90% / 100%）。</li>
 *   <li>抽取兽种数 {@link #getDrawCount}：未开启苦力蛊[Hardship Strength Gu]时仅在掷中时抽 1 种（未掷中也照常开启空轮计
 *       时、轮内不再补掷）；开启苦力蛊时按连击轮数 {@code streak} 抽取对应数量的不重复兽种（上一轮结束后
 *       {@link #STREAK_WINDOW_TICKS} 内再次开轮则连击数 {@code +1}，超时或关闭苦力蛊则重置为 {@code 1}）。</li>
 *   <li>全面解放判定（{@link #getFullyReleased}）：若当前处于全力以赴状态则本轮抽中的全部兽种均全面解放，否则仅在开轮掷
 *       中时解放首个抽中兽种；全面解放兽种的永久点数经 {@link StrengthService#getReleasedExtra} 放大（仍受当前承受力上
 *       限截断），其移速加成同步借给 {@link SprintSpeedService}；轮次开启或中途失去某兽种时发布
 *       {@link BeastPhantomsDrawnEvent} 刷新虚影实体。</li>
 *   <li>力气蛊[Strength Qi Gu]远程气波受 {@link #RANGE_INTERVAL_TICKS}（0.5 秒）发射间隔限制（{@link #canRange}）：同一
 *       轮内的两记气波共用本轮抽中的兽种，若在轮外发射则先调 {@link #startRange} 开启新轮次。</li>
 *   <li>⚠ 轮次与远程冷却状态仅保存在服务端内存映射中、不落盘，玩家死亡、登出、跨维度或停服时一律清空。</li>
 * </ul>
 */

@EventBusSubscriber(modid = Guzhenren.MOD_ID)
public final class StrengthRound {

    private StrengthRound() {}

    public static final float CHANCE = 0.05F;
    private static final float[] ALL_OUT_CHANCES = { 0.80F, 0.90F, 1.0F };
    public static final int ROUND_TICKS = 20;
    public static final int RANGE_INTERVAL_TICKS = 10;
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
        return canRange(RANGED.get(player.getUUID()), player.level().getGameTime());
    }

    public static boolean canRange(@Nullable Long lastRanged, long now) {
        return lastRanged == null || now - lastRanged >= RANGE_INTERVAL_TICKS;
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
