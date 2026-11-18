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
 * 空窍温养[Nourish]、冲刷窍壁与石窍转化的静态服务：玩家提升小境界与转数的唯一业务入口。
 * <ul>
 *   <li>温养进度与石化标记存于各 {@link Aperture}，费用经模组入口注入的 {@link StonePurse} 支付（先扣真元、不足再拿
 *       元石[Primeval Stone]补足）；升阶或升转落盘后发布 {@link RealmReachedEvent} 驱动进度触发器。</li>
 *   <li>⚠ 自身时间加速通过增加单次心跳执行的秒步数（{@link #tickNourish} 遍历 {@code beat.getSteps()}）实现，而非缩放
 *       单秒进度与单秒耗费，以免取整误差导致一轮温养的总真元消耗偏离整池定义；每步经
 *       {@link ApertureEssenceService#consumeDeferred} 延迟写入目标空窍，与新进度合并为一次落盘。</li>
 *   <li>{@link #shiftTargetForInsertedFirst}：希望蛊[Hope Gu]在下标 {@code 0} 插入第一空窍时，若正在温养原下标
 *       {@code 0} 的孤立第二空窍，将目标下标同步后移到 {@code SECOND}；未在温养则不改，留待下次 {@link #start} 写
 *       入。</li>
 *   <li>{@link #start} 经 {@link #canNourish} 拒绝越界下标；无空窍时 {@link #getTargetIndex} 返回 {@code PRIMARY}（避
 *       免按 {@code count - 1} 夹取出 {@code -1}），且心跳会立即取消温养。</li>
 *   <li>⚠ 只有主空窍能冲刷窍壁，第二空窍只能温养升阶、靠更高转第二空窍蛊[Second Aperture Gu]升转；冲刷先扣费后掷骰，成
 *       败均清零温养进度，成功升转时必须同时将阶段重置为 {@link Stage#LOWEST}（否则残留巅峰倍率会让真元上限暴
 *       增）。</li>
 *   <li>⚠ {@link #resolve} 是供单元测试校验的纯函数，按 {@code 0..99} 骰点查普通或十绝结果表：两表分界点不同，且只有
 *       十绝表永不触发跌落基础真元（{@link Outcome#DROP_BASE}）。</li>
 *   <li>⚠ {@link #petrify} 是 {@code petrified} 的唯一写入入口：将目标空窍推至本转巅峰、清零进度与压力并锁死修炼，同时
 *       停止当前温养；此后唯有十绝体满压转化（{@link #convertPetrifiedPressure}，升入下一转初阶、解除石化并将压力置为
 *       {@link #CONVERTED_PRESSURE}，可继续用下一转石窍蛊[Stone Aperture Gu]循环；五转无下一转时返回 {@code false} 交由
 *       调用方引爆）或角色重置才能解除石化。</li>
 *   <li>本类与冲刷、石窍共用元石袋和进度状态而保持同文件；可变静态字段 {@code stones} 仅在模组构造时由入口类注入一
 *       次。</li>
 * </ul>
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
