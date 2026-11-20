package net.alex.guzhenren.gameplay.path.strength;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import net.alex.guzhenren.Guzhenren;
import net.alex.guzhenren.core.ModEvents;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * 连招轮[Combo Round]：力道暴击[Crit]的结算单位，一轮即 Epic Fight 战斗模式下的一套空手连招，只在最后一下（收尾招）掷
 * 一次暴击。
 * <ul>
 *   <li>轮的边界不计时，全部取自连招：{@code EpicFightIntegration} 在每个空手出招起播时调 {@link #beginStrike} 并告知
 *       是否收尾招（连段最后一段；冲刺拳、空中拳一轮只有一下，自身即收尾），收尾招最后一段判定结束时调
 *       {@link #complete}，连段中途被 Epic Fight 清零（超时、闪避、受击等其他动作）时调 {@link #abandon}；上一个收尾招
 *       未结束就起了新招同样按中断计。采矿模式的原版拳不出招，因此不开轮。前几段是普通拳。</li>
 *   <li>一轮一次暴击掷骰（{@link #rollsCrit}）：首次查询时按 {@link StrengthCrit#getChance} 掷定，之后复用；收尾招命中
 *       时由 {@link #strike} 掷，挥空则由收尾结束时的 {@link #rollsArea} 掷。兽力虚影流[Beast Strength Phantom]暴击时
 *       从永久兽种中抽 {@link StrengthCrit#getCount} 种（仅有限时兽力时不抽）；人力钧力流[Human Jun Strength]暴击时
 *       {@link #rollsArea} 为真，放范围伤害[Area Damage]。</li>
 *   <li>抽中兽种的永久点数在结算力量点[Strength Points]里再计一份（{@link #getCritPoints}），只在收尾招进行中计入：
 *       {@link #strike} 把这份增量直接补进本击伤害，同时立即刷新攻击属性供同招其余目标与范围伤害读取，收尾结束再刷新撤
 *       掉。抽取即发布 {@link BeastPhantomsDrawnEvent} 出虚影，暴击兽种的移速加成在虚影显示的 {@link #SHOWN_TICKS} 内借
 *       给 {@link SprintSpeedService}（{@link #getCritFamilies}，失去资格的兽种实时剔除）。</li>
 *   <li>收尾招首次近战命中时记下溢出量（{@link #markLanded}，本击护甲前伤害减去目标挨打前生命），目标举盾挡下时
 *       {@link #markBlocked} 先扣掉格挡量，完全挡下则溢出为 {@code 0}；范围伤害读 {@link #getSpill}（未命中时为整拳伤
 *       害）。</li>
 *   <li>出拳判定：{@link #markPunch} 标记本 tick 是一次出拳（原版挥拳与 Epic Fight 出招各由事件入口调用），暴击掷骰与过
 *       载反噬只认 {@link #isPunch} 为真的 {@code minecraft:player_attack} 伤害，把同类型的二段飞行冲撞挡在外面。</li>
 *   <li>{@link #settleOverload} 让每次出手只结算一次过载反噬：新出招（{@link #beginStrike}）重新放行，同一招或不出招
 *       的原版拳在 {@link #OVERLOAD_GATE_TICKS} 内的后续命中不再结算（{@link #claimOverload}）；这一招已发出远程攻击
 *       （{@link #markShot}）时，近战命中与弹射物爆开共用那一份 {@link BodyOverload.Shot}，{@link #SHOT_TICKS} 后失
 *       效。收尾招发出远程攻击（{@link #markRanged}）时不再放近战锥形。</li>
 *   <li>⚠ 轮次状态只存在服务端内存、不落盘，死亡、登出、跨维度或停服一律清空。</li>
 * </ul>
 */

@EventBusSubscriber(modid = Guzhenren.MOD_ID)
public final class ComboRound {

    private ComboRound() {}

    public static final int SHOWN_TICKS = 20;
    public static final int OVERLOAD_GATE_TICKS = 10;
    public static final int SHOT_TICKS = 40;
    private static final Map<UUID, State> STATES = new HashMap<>();

    private static final class State {

        private @Nullable Finisher finisher;
        private List<BeastStrengthFamily> drawn = new ArrayList<>();
        private long shownUntil = Long.MIN_VALUE;
        private long overloadUntil = Long.MIN_VALUE;
        private @Nullable BodyOverload.Shot shot;
        private long shotUntil = Long.MIN_VALUE;
        private long punchTick = Long.MIN_VALUE;

        private boolean isIdle(long now) {
            return finisher == null && now >= shownUntil && now >= overloadUntil && now >= shotUntil
                    && now > punchTick;
        }
    }

    private static final class Finisher {

        private boolean landed;
        private boolean struck;
        private boolean ranged;
        private @Nullable Boolean crit;
        private @Nullable Float spill;
        private int spillTarget = -1;
        private boolean blocked;
    }

    //region Round [轮次]
    public static void beginStrike(@NotNull ServerPlayer player, boolean finishing) {
        State state = STATES.computeIfAbsent(player.getUUID(), id -> new State());
        boolean crit = hasCritPoints(state);
        state.finisher = finishing ? new Finisher() : null;
        state.overloadUntil = Long.MIN_VALUE;
        state.shot = null;
        state.shotUntil = Long.MIN_VALUE;
        if (crit) AttackDamageService.refresh(player);
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
        if (finisher.struck) return 0.0F;

        finisher.struck = true;
        boolean crit = rollsCrit(player, roll);
        if (PathStrengthService.getHeldBranch(player) != StrengthPathBranch.BEAST_STRENGTH_PHANTOM) {
            state.drawn = new ArrayList<>();
            return 0.0F;
        }

        PathStrengthData data = PathStrengthService.get(player);
        List<BeastStrengthFamily> available = StrengthCrit.getAvailableFamilies(data);
        int count = crit ? StrengthCrit.getCount(HardshipStrength.getTier(player), available.size()) : 0;
        state.drawn = StrengthCrit.pickFamilies(available, count, player.getRandom());
        state.shownUntil = player.level().getGameTime() + SHOWN_TICKS;
        if (!available.isEmpty()) ModEvents.post(new BeastPhantomsDrawnEvent(player, state.drawn));

        StrengthSnapshot snapshot = StrengthService.snapshot(player);
        int gained = snapshot.settle(StrengthCrit.getCritPoints(state.drawn, data), 1.0D) - snapshot.settledPoints();
        if (gained > 0) AttackDamageService.refresh(player);
        return (float) (gained * PathStrengthData.ATTACK_PER_POINT);
    }

    public static boolean rollsCrit(@NotNull ServerPlayer player, float roll) {
        State state = STATES.get(player.getUUID());
        if (state == null || state.finisher == null) return false;

        if (state.finisher.crit == null) state.finisher.crit = roll < StrengthCrit.getChance(player);
        return state.finisher.crit;
    }

    public static boolean rollsCrit(@NotNull ServerPlayer player) {
        return rollsCrit(player, player.getRandom().nextFloat());
    }

    public static boolean rollsArea(@NotNull ServerPlayer player) {
        return PathStrengthService.getHeldBranch(player) == StrengthPathBranch.HUMAN_JUN_STRENGTH
                && rollsCrit(player);
    }

    public static void markLanded(@NotNull Player player, @NotNull LivingEntity target, float damage, float health) {
        State state = STATES.get(player.getUUID());
        if (state == null || state.finisher == null || state.finisher.spill != null) return;

        state.finisher.spill = AreaDamage.getSpill(damage, health);
        state.finisher.spillTarget = target.getId();
    }

    public static void markBlocked(@NotNull Player player, @NotNull LivingEntity target, float damage, float health,
                                   float blocked) {
        State state = STATES.get(player.getUUID());
        if (state == null || state.finisher == null || state.finisher.blocked) return;
        if (state.finisher.spillTarget != target.getId() || blocked <= 0.0F) return;

        state.finisher.blocked = true;
        state.finisher.spill = blocked >= damage ? 0.0F : AreaDamage.getSpill(damage - blocked, health);
    }

    public static float getSpill(@NotNull Player player, float damage) {
        State state = STATES.get(player.getUUID());
        return state == null || state.finisher == null || state.finisher.spill == null ? damage : state.finisher.spill;
    }

    public static void markRanged(@NotNull ServerPlayer player) {
        State state = STATES.get(player.getUUID());
        if (state != null && state.finisher != null) state.finisher.ranged = true;
    }

    public static boolean isRanged(@NotNull Player player) {
        State state = STATES.get(player.getUUID());
        return state != null && state.finisher != null && state.finisher.ranged;
    }

    public static void complete(@NotNull ServerPlayer player) {
        State state = STATES.get(player.getUUID());
        if (state == null || state.finisher == null) return;

        boolean crit = hasCritPoints(state);
        state.finisher = null;
        if (crit) AttackDamageService.refresh(player);
    }

    public static void abandon(@NotNull ServerPlayer player) {
        State state = STATES.get(player.getUUID());
        if (state == null) return;

        boolean crit = hasCritPoints(state);
        state.finisher = null;
        if (crit) AttackDamageService.refresh(player);
    }

    public static void clear(@NotNull Player player) { STATES.remove(player.getUUID()); }

    private static boolean hasCritPoints(State state) {
        return state.finisher != null && state.finisher.struck && !state.drawn.isEmpty();
    }
    //endregion

    //region Punch [出拳]
    public static void markPunch(@NotNull ServerPlayer player) {
        STATES.computeIfAbsent(player.getUUID(), id -> new State()).punchTick = player.level().getGameTime();
    }

    public static boolean isPunch(@NotNull Player player) {
        State state = STATES.get(player.getUUID());
        return state != null && state.punchTick == player.level().getGameTime();
    }
    //endregion

    //region Overload [过载]
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
    //endregion

    //region Crit [暴击]
    public static int getCritPoints(@NotNull Player player) {
        State state = STATES.get(player.getUUID());
        if (state == null || !hasCritPoints(state)) return 0;
        return StrengthCrit.getCritPoints(state.drawn, PathStrengthService.get(player));
    }

    public static @NotNull List<BeastStrengthFamily> getFamilies(@NotNull Player player) {
        State state = STATES.get(player.getUUID());
        return state != null && state.finisher != null && state.finisher.struck ? List.copyOf(state.drawn) : List.of();
    }

    public static @NotNull List<BeastStrengthFamily> getShown(@NotNull Player player) {
        State state = STATES.get(player.getUUID());
        return state != null && isShowing(state, player.level().getGameTime()) ? List.copyOf(state.drawn) : List.of();
    }

    public static @NotNull List<BeastStrengthFamily> getCritFamilies(@NotNull Player player) {
        State state = STATES.get(player.getUUID());
        if (state == null || !isShowing(state, player.level().getGameTime())) return List.of();

        List<BeastStrengthFamily> held = new ArrayList<>(state.drawn);
        PathStrengthData data = PathStrengthService.get(player);
        held.removeIf(family -> data.getBeastPoints(family) <= 0);
        return held;
    }

    private static boolean isShowing(State state, long now) { return now < state.shownUntil; }
    //endregion

    //region Event [事件]
    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;

        State state = STATES.get(player.getUUID());
        if (state != null && state.isIdle(player.level().getGameTime())) STATES.remove(player.getUUID());
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
    //endregion
}
