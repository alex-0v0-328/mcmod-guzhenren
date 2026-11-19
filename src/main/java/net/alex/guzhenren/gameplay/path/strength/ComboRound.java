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
 * 连招轮[Combo Round]：兽力虚影流[Beast Strength Phantom]与范围冲击[Area Impact]的结算单位，一轮即 Epic Fight 战斗模式下
 * 的一套空手连招，只在最后一下结算（在通用初始化时注册为 {@link StrengthService.ReleaseSource}）。
 * <ul>
 *   <li>轮的边界不计时，全部取自连招：{@code EpicFightIntegration} 在每个空手出招起播时调 {@link #beginStrike} 并告知
 *       是否收尾招（连段最后一段；冲刺拳、空中拳一轮只有一下，自身即收尾），收尾招最后一段判定结束时调
 *       {@link #complete}，连段中途被 Epic Fight 清零（超时、闪避、受击等其他动作）时调 {@link #abandon}，被冲刺或空中
 *       收尾招本身清零时只调 {@link #breakStreak}；上一个收尾招未结束就起了新招同样按中断计。采矿模式的原版拳不出招，因此
 *       不开轮。</li>
 *   <li>收尾招命中时调 {@link #strike}（同一招打中多个目标只抽一次）：按 {@link #getChance} 掷骰（平时
 *       {@link #CHANCE} 加兽力加成，全力以赴三/四/五转 80% / 90% / 100%），掷中才从永久兽力[Beast Strength]兽种中抽取
 *       （仅有限时兽力时不抽）：未开苦力蛊[Hardship Strength Gu]抽 1 种，开苦力时抽 {@code min(连击数, 兽种数)} 种，
 *       连击数 = 此前连续打完的轮数 + 1，中断或苦力关闭即回到 1。前几段是普通拳。</li>
 *   <li>抽中的兽种一律全面解放（{@link #getFullyReleased}），其永久点数在结算力量点[Strength Points]里再计一份
 *       （{@link #getReleasedPoints}），只在收尾招进行中计入：抽中那一击直接补进伤害，同时立即刷新攻击属性供同招其余
 *       目标与范围冲击读取，收尾结束再刷新撤掉。抽中即发布 {@link BeastPhantomsDrawnEvent} 出虚影，解放兽种的移速加成
 *       在虚影显示的 {@link #SHOWN_TICKS} 内借给 {@link SprintSpeedService}，失去资格的兽种实时剔除。</li>
 *   <li>收尾招首次近战命中时记下溢出量（{@link #markLanded}，本击护甲前伤害减去目标挨打前生命），供范围冲击读取
 *       （{@link #getSpill}，未命中时为整拳伤害）。</li>
 *   <li>{@link #settleOverload} 让每次出手只结算一次过载反噬：新出招（{@link #beginStrike}）重新放行，同一招或不出招
 *       的原版拳在 {@link #OVERLOAD_GATE_TICKS} 内的后续命中不再结算（{@link #claimOverload}）；这一招已发出远程拳
 *       （{@link #markShot}）时，近战命中与弹射物爆开共用那一份 {@link BodyOverload.Shot}，{@link #SHOT_TICKS} 后失
 *       效。</li>
 *   <li>范围冲击每轮只掷一次（{@link #rollsImpact}，首次查询时掷、之后复用）：近战收尾在最后一段结束时放锥形冲击；收尾
 *       招发出远程拳（{@link #markRanged}）时改由远程拳爆开放球形冲击，近战锥形不再放。</li>
 *   <li>⚠ 轮次状态只存在服务端内存、不落盘，死亡、登出、跨维度或停服一律清空。</li>
 * </ul>
 */

@EventBusSubscriber(modid = Guzhenren.MOD_ID)
public final class ComboRound {

    private ComboRound() {}

    public static final float CHANCE = 0.05F;
    private static final float[] ALL_OUT_CHANCES = { 0.80F, 0.90F, 1.0F };
    public static final int SHOWN_TICKS = 20;
    public static final int OVERLOAD_GATE_TICKS = 10;
    public static final int SHOT_TICKS = 40;
    private static final Map<UUID, State> STATES = new HashMap<>();

    private static final class State {

        private int streak;
        private @Nullable Finisher finisher;
        private List<BeastStrengthFamily> drawn = new ArrayList<>();
        private long shownUntil = Long.MIN_VALUE;
        private long overloadUntil = Long.MIN_VALUE;
        private @Nullable BodyOverload.Shot shot;
        private long shotUntil = Long.MIN_VALUE;

        private boolean isIdle(long now) {
            return streak == 0 && finisher == null && now >= shownUntil && now >= overloadUntil && now >= shotUntil;
        }
    }

    private static final class Finisher {

        private boolean landed;
        private boolean drawn;
        private boolean ranged;
        private @Nullable Boolean impact;
        private @Nullable Float spill;
    }

    //region Round [轮次]
    public static void beginStrike(@NotNull ServerPlayer player, boolean finishing) {
        State state = STATES.computeIfAbsent(player.getUUID(), id -> new State());
        boolean released = state.finisher != null && state.finisher.drawn;
        if (state.finisher != null) state.streak = 0;
        state.finisher = finishing ? new Finisher() : null;
        state.overloadUntil = Long.MIN_VALUE;
        state.shot = null;
        state.shotUntil = Long.MIN_VALUE;
        if (released) AttackDamageService.refresh(player);
    }

    public static boolean isFinishing(@NotNull Player player) {
        State state = STATES.get(player.getUUID());
        return state != null && state.finisher != null;
    }

    public static float strike(@NotNull ServerPlayer player, float roll) {
        State state = STATES.get(player.getUUID());
        if (state == null || state.finisher == null) return 0.0F;

        Finisher finisher = state.finisher;
        finisher.landed = true;
        if (finisher.drawn) return 0.0F;

        finisher.drawn = true;
        PathStrengthData data = PathStrengthService.get(player);
        List<BeastStrengthFamily> available = getAvailableFamilies(data);
        boolean hardship = HardshipStrength.isActive(player);
        boolean rolled = roll < getChance(StrengthService.getAllOutTier(player), data.getBeastChanceBonus());
        int count = available.isEmpty() ? 0 : getDrawCount(hardship, rolled, state.streak + 1, available.size());
        state.drawn = pickFamilies(available, count, player.getRandom());
        state.shownUntil = player.level().getGameTime() + SHOWN_TICKS;
        if (!available.isEmpty()) ModEvents.post(new BeastPhantomsDrawnEvent(player, state.drawn));

        StrengthSnapshot snapshot = StrengthService.snapshot(player);
        int gained = snapshot.settle(getReleasedPoints(state.drawn, data), 1.0D) - snapshot.settledPoints();
        if (gained > 0) AttackDamageService.refresh(player);
        return (float) (gained * PathStrengthData.ATTACK_PER_POINT);
    }

    public static void markLanded(@NotNull Player player, float damage, float health) {
        State state = STATES.get(player.getUUID());
        if (state == null || state.finisher == null || state.finisher.spill != null) return;
        state.finisher.spill = AreaImpact.getSpill(damage, health);
    }

    public static float getSpill(@NotNull Player player, float damage) {
        State state = STATES.get(player.getUUID());
        return state == null || state.finisher == null || state.finisher.spill == null ? damage : state.finisher.spill;
    }

    public static boolean claimOverload(@NotNull ServerPlayer player) {
        State state = STATES.computeIfAbsent(player.getUUID(), id -> new State());
        long now = player.level().getGameTime();
        if (now < state.overloadUntil) return false;

        state.overloadUntil = now + OVERLOAD_GATE_TICKS;
        return true;
    }

    public static void markShot(@NotNull ServerPlayer player, @NotNull BodyOverload.Shot shot) {
        State state = STATES.computeIfAbsent(player.getUUID(), id -> new State());
        state.shot = shot;
        state.shotUntil = player.level().getGameTime() + SHOT_TICKS;
    }

    public static float settleOverload(@NotNull ServerPlayer player) {
        State state = STATES.get(player.getUUID());
        if (state != null && state.shot != null && player.level().getGameTime() < state.shotUntil) {
            return state.shot.settle(player);
        }
        return claimOverload(player) ? BodyOverload.recoil(player) : 0.0F;
    }

    public static void complete(@NotNull ServerPlayer player, boolean cut) {
        State state = STATES.get(player.getUUID());
        if (state == null || state.finisher == null) return;

        Finisher finisher = state.finisher;
        state.finisher = null;
        state.streak = getNextStreak(state.streak, HardshipStrength.isActive(player), !cut || finisher.landed);
        if (finisher.drawn) AttackDamageService.refresh(player);
    }

    public static void abandon(@NotNull ServerPlayer player) {
        State state = STATES.get(player.getUUID());
        if (state == null) return;

        boolean released = state.finisher != null && state.finisher.drawn;
        state.finisher = null;
        state.streak = 0;
        if (released) AttackDamageService.refresh(player);
    }

    public static void breakStreak(@NotNull ServerPlayer player) {
        State state = STATES.get(player.getUUID());
        if (state != null) state.streak = 0;
    }

    public static boolean rollsImpact(@NotNull ServerPlayer player) {
        State state = STATES.get(player.getUUID());
        if (state == null || state.finisher == null) return false;

        if (state.finisher.impact == null) state.finisher.impact = AreaImpact.rolls(player, player.getRandom());
        return state.finisher.impact;
    }

    public static void markRanged(@NotNull ServerPlayer player) {
        State state = STATES.get(player.getUUID());
        if (state != null && state.finisher != null) state.finisher.ranged = true;
    }

    public static boolean isRanged(@NotNull Player player) {
        State state = STATES.get(player.getUUID());
        return state != null && state.finisher != null && state.finisher.ranged;
    }

    public static int getStreak(@NotNull Player player) {
        State state = STATES.get(player.getUUID());
        return state == null ? 0 : state.streak;
    }

    public static @NotNull List<BeastStrengthFamily> getFamilies(@NotNull Player player) {
        State state = STATES.get(player.getUUID());
        return state != null && state.finisher != null && state.finisher.drawn ? List.copyOf(state.drawn) : List.of();
    }

    public static @NotNull List<BeastStrengthFamily> getShown(@NotNull Player player) {
        State state = STATES.get(player.getUUID());
        return state != null && isShowing(state, player.level().getGameTime()) ? List.copyOf(state.drawn) : List.of();
    }

    public static void clear(@NotNull Player player) { STATES.remove(player.getUUID()); }

    private static boolean isShowing(State state, long now) { return now < state.shownUntil; }

    public static int getNextStreak(int streak, boolean hardship, boolean finished) {
        return hardship && finished ? streak + 1 : 0;
    }

    public static float getChance(int allOutTier, float beastBonus) {
        if (allOutTier < StrengthService.ALL_OUT_LOWEST_TIER) return CHANCE + beastBonus;
        return ALL_OUT_CHANCES[Math.min(allOutTier - StrengthService.ALL_OUT_LOWEST_TIER, ALL_OUT_CHANCES.length - 1)];
    }

    public static int getDrawCount(boolean hardship, boolean rolled, int streak, int families) {
        if (!rolled) return 0;
        return Math.min(hardship ? Math.max(1, streak) : 1, families);
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
    public static int getReleasedPoints(@NotNull Player player) {
        State state = STATES.get(player.getUUID());
        if (state == null || state.finisher == null || !state.finisher.drawn) return 0;
        return getReleasedPoints(state.drawn, PathStrengthService.get(player));
    }

    public static int getReleasedPoints(@NotNull List<BeastStrengthFamily> families,
                                        @NotNull PathStrengthData permanent) {
        int points = 0;
        for (BeastStrengthFamily family : families) points += Math.max(0, permanent.getBeastPoints(family));
        return points;
    }

    public static @NotNull List<BeastStrengthFamily> getFullyReleased(@NotNull Player player) {
        State state = STATES.get(player.getUUID());
        if (state == null || !isShowing(state, player.level().getGameTime())) return List.of();

        List<BeastStrengthFamily> held = new ArrayList<>(state.drawn);
        PathStrengthData data = PathStrengthService.get(player);
        held.removeIf(family -> data.getBeastPoints(family) <= 0);
        return held;
    }
    //endregion

    //region Event [事件]
    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;

        State state = STATES.get(player.getUUID());
        if (state == null) return;

        if (!HardshipStrength.isActive(player)) state.streak = 0;
        if (state.isIdle(player.level().getGameTime())) STATES.remove(player.getUUID());
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
    public static void onServerStopped(ServerStoppedEvent event) { STATES.clear(); }

    @SubscribeEvent
    public static void onCommonSetup(FMLCommonSetupEvent event) {
        StrengthService.setReleaseSource(player -> player instanceof ServerPlayer ? getReleasedPoints(player) : 0);
    }
    //endregion
}
