package net.alex.guzhenren.gameplay.aperture;

import net.alex.guzhenren.core.ActionBar;
import net.alex.guzhenren.core.ModEvents;
import net.alex.guzhenren.gameplay.aperture.awakening.AwakeningRitualService;
import net.alex.guzhenren.gameplay.body.BodyService;
import net.alex.guzhenren.gameplay.clock.Beat;
import net.alex.guzhenren.gameplay.clock.TimeFlow;
import net.alex.guzhenren.gameplay.data.PlayerData;
import net.alex.guzhenren.item.GuItem.Refusal;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * 温养[Nourish]空窍[Aperture]与冲刷窍壁服务，转数提升的唯一途径。
 *
 * <ul>
 *   <li>静态服务，进度与石化锁存在各 {@link Aperture} 上；冲刷费经入口类设的 {@link StonePurse} 付，先真元[Essence]后元
 *       石[Primeval Stone]；升阶、升转写入空窍后发布 {@link RealmReachedEvent}，进度包据此判境界进度。</li>
 *   <li>⚠ 加速时钟让每次心跳计费更多秒（{@link #tickNourish}，{@code steps}），不放大一秒：缩放进度与价格会因取整让一轮
 *       长度偏离其定义所耗的一池真元；计费的一秒只写一次目标空窍，{@code pay} 不写
 *       它（{@link ApertureEssenceService#consumeDeferred}），进度与所耗真元一起落盘。</li>
 *   <li>{@link #shiftTargetForInsertedFirst}：希望蛊[Hope Gu]在位置 0 插入第一空窍时，瞄准孤立第二空窍（目标 0）的温养
 *       会滑到新第一空窍，故目标随空窍移动；未在温养则不动，下次 {@code start} 重写。</li>
 *   <li>目标不越界：{@code start} 拒绝列表外下标（{@link #canNourish}）；无空窍时 {@link #getTargetIndex} 返回
 *       {@code PRIMARY}，因按 {@code count - 1} 夹取得 -1，且心跳会取消进行中的温养。</li>
 *   <li>⚠ 窍壁只属 {@code PRIMARY} 空窍：第二空窍能温养不能冲刷，只靠更高转的第二空窍蛊[Second Aperture Gu]升转；⚠ 升转
 *       须同时把阶段重置为 {@code LOWEST}，否则「二转巅峰」使真元上限平方；⚠ 冲刷成败都清零进度，先收费后掷骰。</li>
 *   <li>{@link #IMPACT_COST_PER_RANK_BASE}：一次冲刷费十绝天资巅峰真元池的 1.5 倍，任何池都装不下；{@link Outcome} 是一
 *       次冲刷的结果；{@link #resolve} 是单元测试钉住的接缝，{@code 0} 至 {@code 99} 的骰点对照两张结果表；☠ 两表分界点
 *       不同，只有十绝那张永不掉基础真元。</li>
 *   <li>⚠ {@link #petrify} 是 {@code petrified} 的写入方：空窍落到本转巅峰、压力清零、锁住修炼，直到满压力转化（十绝
 *       体）或 {@code resetAll} 清除；同一次写入强停温养，心跳循环无需另查石化。</li>
 *   <li>{@link #convertPetrifiedPressure}：石化空窍持续积压，满时转为下一转初阶并解除石化，是 {@code resetAll} 外回到
 *       {@code NORMAL} 的唯一途径，落地后可用下一转石窍蛊[Stone Aperture Gu]重入循环；无下一转时返回 {@code false}，由
 *       调用方引爆；{@link #CONVERTED_PRESSURE} 是转化后的压力落点。</li>
 *   <li>超 300 行：温养与冲刷共用元石袋与进度；可变静态 {@code stones} 是接缝，入口类仅在模组构造时设一次。</li>
 * </ul>
 *
 * @author Alex
 * @version 1.0.0
 * @see ApertureService
 * @see TimeFlow
 * @since 1.0.0
 */

public final class ApertureNourishService {

    private ApertureNourishService() {}

    public static final int PERCENT_PER_SECOND = 1;
    public static final int COST_DIVISOR = 100;
    public static final int BASE_LOSS_MIN = 1;
    public static final int BASE_LOSS_MAX = 5;
    public static final long IMPACT_COST_PER_RANK_BASE = 1_200L;
    public static final int CONVERTED_PRESSURE = 90;
    private static final int STAGE_UP_PRESSURE_RELIEF = 20;
    private static final int IMPACT_PRESSURE_RELIEF = 50;
    private static final String STAGE_UP = "guzhenren.nourish.stage_up";
    private static final String IMPACT_SUCCESS = "guzhenren.impact.success";
    private static final String IMPACT_HOLD = "guzhenren.impact.hold";
    private static final String IMPACT_DROP_STAGE = "guzhenren.impact.drop_stage";
    private static final String IMPACT_DROP_BASE = "guzhenren.impact.drop_base";
    private static StonePurse stones;

    public enum Outcome { SUCCESS, HOLD, DROP_STAGE, DROP_BASE }

    public static void setStonePurse(@NotNull StonePurse purse) { stones = purse; }

    public static @NotNull ApertureNourishData get(@NotNull Player player) {
        return PlayerData.get(player).getNourish();
    }

    public static boolean isCultivating(@NotNull Player player) { return get(player).cultivating(); }

    public static float getFraction(@NotNull Player player, int index) {
        return ApertureService.getAperture(player, index).nourishProgress() / (float) ApertureNourishData.FULL;
    }

    public static int getTargetIndex(@NotNull Player player) {
        int count = ApertureService.get(player).count();
        return count == 0 ? ApertureData.PRIMARY : Mth.clamp(get(player).target(), ApertureData.PRIMARY, count - 1);
    }

    //region what the screen asks [界面读数]
    public static boolean canNourish(@NotNull Player player, int index) {
        if (!ApertureService.hasAperture(player) || isCultivating(player)
                || AwakeningRitualService.holdsStill(player)) return false;
        if (index < 0 || index >= ApertureService.get(player).count()) return false;
        if (ApertureService.getStatus(player, index) != ApertureStatus.NORMAL) return false;
        return !isAtCeiling(player, index)
                && ApertureService.getAperture(player, index).nourishProgress() < ApertureNourishData.FULL;
    }

    public static boolean canImpact(@NotNull Player player) {
        Aperture aperture = ApertureService.getAperture(player);
        return ApertureService.isAwakened(player) && !isCultivating(player)
                && ApertureService.getStatus(player) == ApertureStatus.NORMAL
                && aperture.nourishProgress() >= ApertureNourishData.FULL
                && aperture.stage() == Stage.HIGHEST && aperture.rank() != Rank.HIGHEST;
    }

    public static boolean isAtCeiling(@NotNull Player player, int index) {
        Aperture aperture = ApertureService.getAperture(player, index);
        return aperture.second() ? aperture.stage() == Stage.HIGHEST
                : aperture.rank() == Rank.HIGHEST && aperture.stage() == Stage.HIGHEST;
    }
    //endregion

    public static long getCostPerSecond(@NotNull Player player, int index) {
        long max = ApertureService.getAperture(player, index).getMaxEssence();
        return Math.max(1L, (max + COST_DIVISOR - 1) / COST_DIVISOR);
    }

    private static long getImpactCost(@NotNull Player player) {
        return IMPACT_COST_PER_RANK_BASE * ApertureService.getAperture(player).rank().getRankBase();
    }

    public static boolean canAffordImpact(@NotNull Player player) {
        return stones.canAfford().test(player, getImpactCost(player));
    }

    public static void start(@NotNull ServerPlayer player, int index) {
        if (!canNourish(player, index)) return;
        store(player, new ApertureNourishData(true, index, ApertureNourishData.NOT_STARVED));
    }

    public static void cancel(@NotNull ServerPlayer player) {
        ApertureNourishData data = get(player);
        if (!data.cultivating()) return;
        store(player, data.stopped());
    }

    public static void shiftTargetForInsertedFirst(@NotNull ServerPlayer player) {
        ApertureNourishData data = get(player);
        if (data.cultivating() && data.target() == ApertureData.PRIMARY) {
            store(player, data.withTarget(ApertureData.SECOND));
        }
    }

    //region nourishing [温养]
    public static void tickNourish(@NotNull ServerPlayer player, @NotNull Beat beat) {
        for (int second = beat.getSteps(); second > 0; second--) {
            if (!nourishSecond(player)) return;
        }
    }

    private static boolean nourishSecond(ServerPlayer player) {
        ApertureNourishData data = get(player);
        if (!data.cultivating()) return false;
        if (!ApertureService.hasAperture(player)) { cancel(player); return false; }
        int target = getTargetIndex(player);
        if (ApertureService.getStatus(player, target) != ApertureStatus.NORMAL
                || isAtCeiling(player, target)) { cancel(player); return false; }

        player.setDeltaMovement(Vec3.ZERO);
        Aperture paid = pay(player, getCostPerSecond(player, target), target);
        if (paid == null) {
            starve(player, data);
            return false;
        }
        return advanceProgress(player, data, target, paid);
    }

    private static void starve(ServerPlayer player, ApertureNourishData data) {
        long now = player.level().getGameTime();
        ApertureNourishData starving = data.isStarving() ? data : data.withStarvedSinceTick(now);
        if (starving.hasStarvedOut(now)) {
            store(player, starving.stopped());
            ActionBar.refuse(player, Refusal.ESSENCE);
            return;
        }
        store(player, starving);
    }

    private static boolean advanceProgress(ServerPlayer player, ApertureNourishData data, int target,
                                           Aperture aperture) {
        Aperture fed = aperture.withNourishProgress(aperture.nourishProgress() + PERCENT_PER_SECOND);
        if (fed.nourishProgress() < ApertureNourishData.FULL) {
            ApertureService.set(player, target, fed);
            store(player, data.withStarvedSinceTick(ApertureNourishData.NOT_STARVED));
            return true;
        }

        Stage stage = aperture.stage();
        if (stage == Stage.HIGHEST) {
            ApertureService.set(player, target, fed);
            store(player, data.stopped());
            return false;
        }
        ApertureService.set(player, target, fed.withStage(stage.shift(1)).withNourishProgress(0));
        if (!ApertureService.getAperture(player, target).second()) {
            AperturePressureService.relieve(player, STAGE_UP_PRESSURE_RELIEF);
            reachRealm(player, target);
        }
        store(player, ApertureNourishData.DEFAULT);
        ActionBar.inform(player, STAGE_UP);
        return false;
    }

    private static @Nullable Aperture pay(ServerPlayer player, long cost, int target) {
        if (player.getAbilities().instabuild) return ApertureService.getAperture(player, target);
        stones.topUp().accept(player);
        return ApertureEssenceService.consumeDeferred(player, cost, target);
    }
    //endregion

    //region Stone Aperture Gu [石窍蛊]
    public static void petrify(@NotNull ServerPlayer player, int index) {
        Aperture aperture = ApertureService.getAperture(player, index);
        if (aperture.petrified()) return;
        ApertureService.set(player, index, aperture.withStage(Stage.HIGHEST)
                .withNourishProgress(0).withPetrified(true));
        reachRealm(player, index);
        AperturePressureService.set(player, index, 0);
        store(player, ApertureNourishData.DEFAULT);
    }

    public static boolean convertPetrifiedPressure(@NotNull ServerPlayer player) {
        Aperture aperture = ApertureService.getAperture(player);
        if (!aperture.petrified() || aperture.rank() == Rank.HIGHEST) return false;

        ApertureService.set(player, ApertureData.PRIMARY, aperture
                .withRank(aperture.rank().shift(1))
                .withStage(Stage.LOWEST)
                .withNourishProgress(0)
                .withPetrified(false));
        reachRealm(player, ApertureData.PRIMARY);
        AperturePressureService.set(player, ApertureData.PRIMARY, CONVERTED_PRESSURE);
        store(player, ApertureNourishData.DEFAULT);
        ActionBar.inform(player, IMPACT_SUCCESS);
        return true;
    }
    //endregion

    //region striking the wall [冲刷窍壁]
    public static void impactWall(@NotNull ServerPlayer player) {
        if (!canImpact(player)) return;
        Aperture aperture = ApertureService.getAperture(player);
        long cost = getImpactCost(player);
        if (!player.getAbilities().instabuild && !stones.spend().test(player, cost)) {
            ActionBar.refuse(player, Refusal.ESSENCE);
            return;
        }

        int roll = player.getRandom().nextInt(100);
        Outcome outcome = resolve(roll, BodyService.isExtreme(player));

        switch (outcome) {
            case SUCCESS -> {
                ApertureService.setRank(player, aperture.rank().shift(1));
                ApertureService.setStage(player, Stage.LOWEST);
                reachRealm(player, ApertureData.PRIMARY);
                AperturePressureService.relieve(player, IMPACT_PRESSURE_RELIEF);
                ActionBar.inform(player, IMPACT_SUCCESS);
            }
            case HOLD -> ActionBar.inform(player, IMPACT_HOLD);
            case DROP_STAGE -> {
                ApertureService.setStage(player, aperture.stage().shift(-1));
                ActionBar.inform(player, IMPACT_DROP_STAGE);
            }
            case DROP_BASE -> {
                int loss = Math.min(aperture.baseEssence() - Aperture.MIN_BASE,
                        BASE_LOSS_MIN + player.getRandom().nextInt(BASE_LOSS_MAX - BASE_LOSS_MIN + 1));
                if (loss > 0) {
                    ApertureService.setBaseEssence(player, aperture.baseEssence() - loss);
                    ActionBar.inform(player, IMPACT_DROP_BASE);
                }
            }
        }
        ApertureService.set(player, ApertureData.PRIMARY,
                ApertureService.getAperture(player, ApertureData.PRIMARY).withNourishProgress(0));
        store(player, ApertureNourishData.DEFAULT);
    }

    public static @NotNull Outcome resolve(int roll, boolean extreme) {
        if (extreme) {
            if (roll < 60) return Outcome.SUCCESS;
            if (roll < 85) return Outcome.HOLD;
            return Outcome.DROP_STAGE;
        }
        if (roll < 40) return Outcome.SUCCESS;
        if (roll < 70) return Outcome.HOLD;
        if (roll < 90) return Outcome.DROP_STAGE;
        return Outcome.DROP_BASE;
    }
    //endregion

    private static void reachRealm(ServerPlayer player, int index) {
        ModEvents.post(new RealmReachedEvent(player, ApertureService.getAperture(player, index)));
    }

    private static void store(ServerPlayer player, ApertureNourishData data) {
        PlayerData.get(player).setNourish(data);
    }
}
