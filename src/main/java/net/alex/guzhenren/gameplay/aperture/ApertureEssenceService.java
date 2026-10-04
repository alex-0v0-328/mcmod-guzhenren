package net.alex.guzhenren.gameplay.aperture;

import com.google.common.math.LongMath;
import java.util.Arrays;
import java.util.List;
import net.alex.guzhenren.Ticks;
import net.alex.guzhenren.effect.pool.EssenceQiEffect;
import net.alex.guzhenren.gameplay.path.time.PathTimeFlowService;
import net.alex.guzhenren.registry.attachment.ModAttachments;
import net.alex.guzhenren.registry.effect.ModEffects;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.player.Player;
import org.jetbrains.annotations.NotNull;

/**
 * Essence [真元]: pools, distilled reserves, regen, and the Liquor Worm's [酒虫] phases. Spending
 * cascades PRIMARY then SECOND, gains fill PRIMARY; {@code regenStep} banks the fractional remainder
 * per aperture in unsynced {@code ESSENCE_CARRY}.
 *
 * <p>⚠ Gates ask {@code spendable()}, never a no-index {@code currentEssence()} (PRIMARY alone, and
 * distilling empties it). ⚠ {@code consume} burns distilled 1:2 first (rounded UP), then the pool. ⚠
 * The distilling truth is the per-aperture {@code distilling} flag, not the effect. ⚠ Any path that
 * SKIPS a regen step (choke, DEAD [死窍]) must zero that carry; {@code isChoked} outranks everything.
 * {@link #cascadeTake} is the pure seam the unit tests pin for that cascade.
 *
 * @author Alex
 * @version 1.0.0
 * @see ApertureService
 * @see PathTimeFlowService
 * @since 1.0.0
 */

public final class ApertureEssenceService {

    private ApertureEssenceService() {}

    public static final long BASE_REGEN_PER_DAY = 100L;
    public static final int REGEN_INTERVAL_TICKS = Ticks.SECOND;

    public static long regenPerDay(@NotNull Aperture aperture) {
        return BASE_REGEN_PER_DAY * aperture.talent().getRegenRate() * aperture.rank().getRankBase()
                * aperture.stage().getEssenceMultiplier();
    }

    public static double regenPerTick(@NotNull Aperture aperture) { return regenPerDay(aperture) / (double) Ticks.DAY; }

    public static final long DISTILLED_RATE = 2L;

    public static long currentEssence(@NotNull Player player) {
        return ApertureService.aperture(player).currentEssence();
    }

    public static long maxEssence(@NotNull Player player) { return ApertureService.aperture(player).maxEssence(); }

    public static long distilledEssence(@NotNull Player player) {
        return ApertureService.aperture(player).distilledEssence();
    }

    public static long totalDistilled(@NotNull Player player) {
        long total = 0L;
        ApertureData data = ApertureService.get(player);
        for (int i = 0; i < data.count(); i++) total += data.get(i).distilledEssence();
        return total;
    }

    public static long spendable(@NotNull Player player) {
        ApertureData data = ApertureService.get(player);
        long total = 0L;
        for (int i = 0; i < data.count(); i++) {
            total += data.get(i).currentEssence() + data.get(i).distilledEssence() * DISTILLED_RATE;
        }
        return total;
    }

    public static boolean isDistilling(@NotNull Player player) { return player.hasEffect(ModEffects.LIQUOR_WORM); }

    public static boolean isChoked(@NotNull Player player) { return player.hasEffect(ModEffects.DEATH_QI); }

    public static double essenceQiBonus(@NotNull Player player) {
        MobEffectInstance effect = player.getEffect(ModEffects.ESSENCE_QI);
        return effect == null ? 0.0 : EssenceQiEffect.bonus(effect.getAmplifier());
    }

    public static void add(@NotNull ServerPlayer player, long delta) {
        long left = delta;
        ApertureData data = ApertureService.get(player);
        for (int i = 0; i < data.count() && left > 0L; i++) {
            Aperture aperture = data.get(i);
            long room = Math.max(0L, aperture.maxEssence() - aperture.currentEssence());
            long given = Math.min(left, room);
            if (given > 0L) set(player, i, aperture.currentEssence() + given);
            left -= given;
        }
    }

    public static void set(@NotNull ServerPlayer player, int index, long value) {
        ApertureService.set(player, index, ApertureService.aperture(player, index).withCurrentEssence(value));
    }

    public static void refill(@NotNull ServerPlayer player) {
        ApertureData data = ApertureService.get(player);
        for (int i = 0; i < data.count(); i++) {
            ApertureService.set(player, i, data.get(i).refilled());
        }
    }

    public static void addDistilled(@NotNull ServerPlayer player, long delta) {
        setDistilled(player, LongMath.saturatedAdd(distilledEssence(player), delta));
    }

    public static void setDistilled(@NotNull ServerPlayer player, long value) {
        setDistilled(player, ApertureData.PRIMARY, value);
    }

    public static void setDistilled(@NotNull ServerPlayer player, int index, long value) {
        ApertureService.set(player, index,
                ApertureService.aperture(player, index).withDistilledEssence(value));
    }

    //region the three phases of a Liquor Worm [酒虫]
    public static boolean canDistill(@NotNull Player player) {
        ApertureData data = ApertureService.get(player);
        for (int i = 0; i < data.count(); i++) {
            if (!data.get(i).distilling()) return true;
        }
        return false;
    }

    public static void beginDistilling(@NotNull ServerPlayer player) {
        ApertureData data = ApertureService.get(player);
        for (int i = 0; i < data.count(); i++) {
            Aperture aperture = data.get(i);
            if (aperture.distilling()) continue;
            ApertureService.set(player, i, aperture.withCurrentEssence(0L).withDistilling(true));
            return;
        }
    }

    public static void endDistilling(@NotNull ServerPlayer player) {
        ApertureData data = ApertureService.get(player);
        for (int i = 0; i < data.count(); i++) {
            Aperture aperture = data.get(i);
            if (!aperture.distilling()) continue;

            long left = aperture.distilledEssence();
            ApertureService.set(player, i, aperture
                    .withCurrentEssence(aperture.currentEssence() + left * DISTILLED_RATE)
                    .withDistilledEssence(0L)
                    .withDistilling(false));
        }
    }
    //endregion

    public static boolean consume(@NotNull ServerPlayer player, long amount) {
        if (amount <= 0L) return true;
        if (spendable(player) < amount) return false;

        ApertureData data = ApertureService.get(player);
        long[][] plan = cascadeTake(amount, data.apertures());
        for (int i = 0; i < plan.length; i++) {
            if (plan[i][0] > 0L) setDistilled(player, i, data.get(i).distilledEssence() - plan[i][0]);
            if (plan[i][1] > 0L) set(player, i, data.get(i).currentEssence() - plan[i][1]);
        }
        return true;
    }

    public static long[][] cascadeTake(long amount, @NotNull List<Aperture> apertures) {
        long[][] takes = new long[apertures.size()][2];
        long left = amount;

        for (int i = 0; i < apertures.size() && left > 0L; i++) {
            Aperture aperture = apertures.get(i);
            long fromDistilled = Math.min(aperture.distilledEssence(),
                    (left + DISTILLED_RATE - 1) / DISTILLED_RATE);
            takes[i][0] = fromDistilled;
            left -= Math.min(left, fromDistilled * DISTILLED_RATE);

            if (left > 0L) {
                long fromCurrent = Math.min(left, aperture.currentEssence());
                takes[i][1] = fromCurrent;
                left -= fromCurrent;
            }
        }
        return takes;
    }

    public static void regenStep(@NotNull ServerPlayer player) {
        ApertureData data = ApertureService.get(player);
        float[] carry = player.getData(ModAttachments.ESSENCE_CARRY);

        if (isChoked(player)) {
            Arrays.fill(carry, 0.0F);
            return;
        }

        double bonus = essenceQiBonus(player);
        for (int index = 0; index < data.count(); index++) {
            regenAperture(player, data.get(index), index, carry, bonus);
        }
    }

    private static void regenAperture(ServerPlayer player, Aperture aperture, int index, float[] carry, double bonus) {
        if (ApertureService.status(player, index) != ApertureStatus.NORMAL) {
            carry[index] = 0.0F;
            return;
        }
        boolean distilling = aperture.distilling();
        long current = distilling ? aperture.distilledEssence() : aperture.currentEssence();
        if (current >= aperture.maxEssence()) {
            carry[index] = 0.0F;
            return;
        }

        double perStep = PathTimeFlowService.perStep(player,
                regenPerTick(aperture) * REGEN_INTERVAL_TICKS * (1.0 + bonus));
        if (perStep <= 0.0) return;
        double total = carry[index] + perStep;
        long whole = (long) total;
        carry[index] = (float) (total - whole);
        if (whole <= 0L) return;

        if (distilling) {
            setDistilled(player, index, current + whole);
        } else {
            set(player, index, current + whole);
        }
    }
}
