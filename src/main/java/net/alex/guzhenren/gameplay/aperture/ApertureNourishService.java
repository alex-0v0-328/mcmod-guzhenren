package net.alex.guzhenren.gameplay.aperture;

import net.alex.guzhenren.gameplay.body.BodyService;
import net.alex.guzhenren.gameplay.path.time.PathTimeFlowService;
import net.alex.guzhenren.item.material.PrimevalStoneItem;
import net.alex.guzhenren.registry.attachment.ModAttachments;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;

/**
 * Nourishing the Aperture [温养空窍] and striking its wall [冲刷窍壁] -- the only way a rank rises.
 * Static service; progress and the petrified latch live on each {@link Aperture}; the strike cost goes
 * through {@link PrimevalStoneItem#spend(ServerPlayer, long)} (essence first, then stones).
 *
 * <p>⚠ The wall is the PRIMARY aperture's alone -- a second aperture nourishes but never strikes; its
 * only rank-up is a higher-rank Second Aperture Gu. ⚠ A rank-up MUST also set the stage back to {@code
 * LOWEST}, or a "二转巅峰" squares the essence cap. ⚠ The strike zeroes progress win or lose; charge
 * BEFORE rolling. ⚠ A hastened clock bills MORE seconds per heartbeat ({@link #tickNourish}, {@code
 * steps}), never a bigger second -- scaling the progress and the price instead would round the
 * round's length off the pool it is defined to cost.
 *
 * <p>{@link #IMPACT_COST_PER_RANK_BASE}: one strike costs one and a half of a Ten-Extremes peak pool,
 * so no pool can ever hold it. {@link #CONVERTED_PRESSURE} is where the pressure gauge lands after a
 * petrified aperture converts a full one into a rank-up. {@link Outcome} is what one strike against
 * the aperture wall did.
 *
 * <p>{@link #shiftTargetForInsertedFirst}: when Hope Gu inserts the first aperture at position 0, an
 * in-progress session aimed at the lone second aperture (target 0) would silently slide onto the new
 * first aperture -- move the target with its aperture. No session running, nothing to move; the next
 * {@code start} rewrites it.
 *
 * <p>⚠ {@link #petrify} is the writer that sets {@code petrified}: it lands the aperture on this
 * rank's peak, zeroes the pressure gauge, and locks cultivation until a full gauge converts
 * (ten-extremes) or resetAll clears it. A run in progress is force-stopped by the same write, so the
 * heartbeat loop needs no petrified check of its own. {@link #convertPetrifiedPressure} is the
 * pressure gauge a petrified aperture keeps filling: at full it converts into the next rank's first
 * stage and cures the stone -- the one way back to NORMAL short of resetAll, and the landing can take
 * the next rank's Stone Aperture Gu to enter the cycle again. Only while a next rank exists; at the
 * last rank it answers {@code false} and the caller detonates.
 *
 * <p>{@link #resolve} is the seam the unit tests pin: a roll of {@code 0..99} against the two outcome
 * tables. ☠ The two tables split at different points, and only the Ten-Extremes one can never lose
 * base.
 *
 * @author Alex
 * @version 1.0.0
 * @see ApertureService
 * @see PathTimeFlowService
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
    private static final String STARVED = "guzhenren.nourish.starved";
    private static final String STAGE_UP = "guzhenren.nourish.stage_up";
    private static final String IMPACT_POOR = "guzhenren.impact.poor";
    private static final String IMPACT_SUCCESS = "guzhenren.impact.success";
    private static final String IMPACT_HOLD = "guzhenren.impact.hold";
    private static final String IMPACT_DROP_STAGE = "guzhenren.impact.drop_stage";
    private static final String IMPACT_DROP_BASE = "guzhenren.impact.drop_base";

    public enum Outcome { SUCCESS, HOLD, DROP_STAGE, DROP_BASE }

    public static @NotNull ApertureNourishData get(@NotNull Player player) {
        return player.getData(ModAttachments.APERTURE_NOURISH);
    }

    public static boolean isCultivating(@NotNull Player player) { return get(player).cultivating(); }

    public static float fraction(@NotNull Player player, int index) {
        return ApertureService.aperture(player, index).nourishProgress() / (float) ApertureNourishData.FULL;
    }

    public static int targetIndex(@NotNull Player player) {
        int count = ApertureService.get(player).count();
        return count == 0 ? ApertureData.PRIMARY : Math.clamp(get(player).target(), ApertureData.PRIMARY, count - 1);
    }

    //region what the screen asks
    public static boolean canNourish(@NotNull Player player, int index) {
        if (!ApertureService.hasAperture(player) || isCultivating(player)) return false;
        if (index < 0 || index >= ApertureService.get(player).count()) return false;
        if (ApertureService.status(player, index) != ApertureStatus.NORMAL) return false;
        return !atCeiling(player, index)
                && ApertureService.aperture(player, index).nourishProgress() < ApertureNourishData.FULL;
    }

    public static boolean canImpact(@NotNull Player player) {
        Aperture aperture = ApertureService.aperture(player);
        return ApertureService.isAwakened(player) && !isCultivating(player)
                && ApertureService.status(player) == ApertureStatus.NORMAL
                && aperture.nourishProgress() >= ApertureNourishData.FULL
                && aperture.stage() == Stage.HIGHEST && aperture.rank() != Rank.HIGHEST;
    }

    public static boolean atCeiling(@NotNull Player player, int index) {
        Aperture aperture = ApertureService.aperture(player, index);
        return aperture.second() ? aperture.stage() == Stage.HIGHEST
                : aperture.rank() == Rank.HIGHEST && aperture.stage() == Stage.HIGHEST;
    }
    //endregion

    public static long costPerSecond(@NotNull Player player, int index) {
        long max = ApertureService.aperture(player, index).maxEssence();
        return Math.max(1L, (max + COST_DIVISOR - 1) / COST_DIVISOR);
    }

    public static long impactCost(@NotNull Player player) {
        return IMPACT_COST_PER_RANK_BASE * ApertureService.aperture(player).rank().getRankBase();
    }

    public static boolean canAffordImpact(@NotNull Player player) {
        return PrimevalStoneItem.canAfford(player, impactCost(player));
    }

    public static void start(@NotNull ServerPlayer player, int index) {
        if (!canNourish(player, index)) return;
        store(player, get(player).withCultivating(true).withTarget(index)
                .withStarvedSinceTick(ApertureNourishData.NOT_STARVED));
    }

    public static void cancel(@NotNull ServerPlayer player) {
        ApertureNourishData data = get(player);
        if (!data.cultivating()) return;
        store(player, data.withCultivating(false).withStarvedSinceTick(ApertureNourishData.NOT_STARVED));
    }

    public static void shiftTargetForInsertedFirst(@NotNull ServerPlayer player) {
        ApertureNourishData data = get(player);
        if (data.cultivating() && data.target() == ApertureData.PRIMARY) {
            store(player, data.withTarget(ApertureData.SECOND));
        }
    }

    //region nourishing [温养] -- the second that the heartbeat bills
    public static void tickNourish(@NotNull ServerPlayer player) {
        for (int second = PathTimeFlowService.steps(player); second > 0; second--) {
            if (!nourishSecond(player)) return;
        }
    }

    private static boolean nourishSecond(ServerPlayer player) {
        ApertureNourishData data = get(player);
        if (!data.cultivating()) return false;
        if (!ApertureService.hasAperture(player)) { cancel(player); return false; }
        int target = targetIndex(player);
        if (ApertureService.status(player, target) != ApertureStatus.NORMAL
                || atCeiling(player, target)) { cancel(player); return false; }

        player.setDeltaMovement(Vec3.ZERO);
        if (!pay(player, costPerSecond(player, target))) {
            starve(player, data);
            return false;
        }
        return advanceProgress(player, data, target);
    }

    private static void starve(ServerPlayer player, ApertureNourishData data) {
        long now = player.level().getGameTime();
        ApertureNourishData starving = data.isStarving() ? data : data.withStarvedSinceTick(now);
        if (starving.starvedOut(now)) {
            store(player, starving.withCultivating(false).withStarvedSinceTick(ApertureNourishData.NOT_STARVED));
            say(player, STARVED);
            return;
        }
        store(player, starving);
    }

    private static boolean advanceProgress(ServerPlayer player, ApertureNourishData data, int target) {
        Aperture aperture = ApertureService.aperture(player, target);
        Aperture fed = aperture.withNourishProgress(aperture.nourishProgress() + PERCENT_PER_SECOND);
        if (fed.nourishProgress() < ApertureNourishData.FULL) {
            ApertureService.set(player, target, fed);
            store(player, data.withStarvedSinceTick(ApertureNourishData.NOT_STARVED));
            return true;
        }

        Stage stage = aperture.stage();
        if (stage == Stage.HIGHEST) {
            ApertureService.set(player, target, fed);
            store(player, data.withCultivating(false).withStarvedSinceTick(ApertureNourishData.NOT_STARVED));
            return false;
        }
        ApertureService.set(player, target, fed.withStage(stage.shift(1)).withNourishProgress(0));
        if (!ApertureService.aperture(player, target).second()) {
            AperturePressureService.relieve(player, STAGE_UP_PRESSURE_RELIEF);
        }
        store(player, ApertureNourishData.DEFAULT);
        say(player, STAGE_UP);
        return false;
    }

    private static boolean pay(ServerPlayer player, long cost) {
        if (player.hasInfiniteMaterials()) return true;
        PrimevalStoneItem.topUp(player);
        return ApertureEssenceService.consume(player, cost);
    }
    //endregion

    //region Stone Aperture Gu [石窍蛊] -- straight to this rank's peak, and never further
    public static void petrify(@NotNull ServerPlayer player, int index) {
        Aperture aperture = ApertureService.aperture(player, index);
        if (aperture.petrified()) return;
        ApertureService.set(player, index, aperture.withStage(Stage.HIGHEST)
                .withNourishProgress(0).withPetrified(true));
        AperturePressureService.set(player, index, 0);
        store(player, ApertureNourishData.DEFAULT);
    }

    public static boolean convertPetrifiedPressure(@NotNull ServerPlayer player) {
        Aperture aperture = ApertureService.aperture(player);
        if (!aperture.petrified() || aperture.rank() == Rank.HIGHEST) return false;

        ApertureService.set(player, ApertureData.PRIMARY, aperture
                .withRank(aperture.rank().shift(1))
                .withStage(Stage.LOWEST)
                .withNourishProgress(0)
                .withPetrified(false));
        AperturePressureService.set(player, ApertureData.PRIMARY, CONVERTED_PRESSURE);
        store(player, ApertureNourishData.DEFAULT);
        say(player, IMPACT_SUCCESS);
        return true;
    }
    //endregion

    //region striking the wall
    public static void impactWall(@NotNull ServerPlayer player) {
        if (!canImpact(player)) return;
        Aperture aperture = ApertureService.aperture(player);
        long cost = impactCost(player);
        if (!player.hasInfiniteMaterials() && !PrimevalStoneItem.spend(player, cost)) {
            player.displayClientMessage(Component.translatable(IMPACT_POOR, cost), true);
            return;
        }

        int roll = player.getRandom().nextInt(100);
        Outcome outcome = resolve(roll, BodyService.isExtreme(player));

        switch (outcome) {
            case SUCCESS -> {
                ApertureService.setRank(player, aperture.rank().shift(1));
                ApertureService.setStage(player, Stage.LOWEST);
                AperturePressureService.relieve(player, IMPACT_PRESSURE_RELIEF);
                say(player, IMPACT_SUCCESS);
            }
            case HOLD -> say(player, IMPACT_HOLD);
            case DROP_STAGE -> {
                ApertureService.setStage(player, aperture.stage().shift(-1));
                say(player, IMPACT_DROP_STAGE);
            }
            case DROP_BASE -> {
                int loss = Math.min(aperture.baseEssence() - Aperture.MIN_BASE,
                        BASE_LOSS_MIN + player.getRandom().nextInt(BASE_LOSS_MAX - BASE_LOSS_MIN + 1));
                if (loss > 0) {
                    ApertureService.setBaseEssence(player, aperture.baseEssence() - loss);
                    say(player, IMPACT_DROP_BASE);
                }
            }
        }
        ApertureService.set(player, ApertureData.PRIMARY,
                ApertureService.aperture(player, ApertureData.PRIMARY).withNourishProgress(0));
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

    private static void say(ServerPlayer player, String key) {
        player.displayClientMessage(Component.translatable(key), true);
    }

    private static void store(ServerPlayer player, ApertureNourishData data) {
        player.setData(ModAttachments.APERTURE_NOURISH, data);
    }
}
