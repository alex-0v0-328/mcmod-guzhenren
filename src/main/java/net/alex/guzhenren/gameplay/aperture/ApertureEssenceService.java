package net.alex.guzhenren.gameplay.aperture;

import com.google.common.math.LongMath;
import java.util.List;
import net.alex.guzhenren.core.Ticks;
import net.alex.guzhenren.gameplay.clock.Beat;
import net.alex.guzhenren.gameplay.clock.TimeFlow;
import net.alex.guzhenren.gameplay.data.PlayerData;
import net.alex.guzhenren.gameplay.path.PathEffectService;
import net.alex.guzhenren.registry.effect.ModPathEffectTypes;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * 空窍真元服务：管理普通真元池、精炼真元、自然回复与酒虫[Liquor Worm]精炼三相。
 * <ul>
 *   <li>扣费按列表顺序（{@code PRIMARY} → {@code SECOND}）级联，补充也按序先填前窍；{@link #regenStep} 将各空窍自然回
 *       复的小数余量存入不同步的 {@code essenceCarry}。</li>
 *   <li>⚠ 跳过自然回复的分支（死窍或已满）必须将该空窍的 {@code essenceCarry} 清零。</li>
 *   <li>⚠ 校验真元是否够用一律调 {@link #getSpendable}，不要调只读主空窍普通池的 {@link #getCurrentEssence}（酒虫精炼
 *       期间普通池会被清空）。</li>
 *   <li>⚠ {@link #consume} 在每个空窍内先按 1:2（{@link #DISTILLED_RATE}，向上取整）扣精炼真元再扣普通真元，每个被扣空
 *       窍只写一次；{@link #consumeDeferred} 同样级联扣费，但跳过写入下标 {@code index} 的空窍并返回扣费后的新实例（不
 *       够付时返回 {@code null}），供调用方合并进同一次落盘；{@link #cascadeTake} 是供单元测试校验级联逻辑的纯函数。
 *       </li>
 *   <li>⚠ 单个空窍是否处于精炼态（自然回复进哪个池、能否再次精炼、退还给谁）以 {@link Aperture#distilling} 为准，而精
 *       炼何时结束以酒虫流派效果为准：{@link #closeDistilling} 在心跳中发现效果已结束但仍有空窍带标记时，将剩余精炼真元
 *       按 1:2 退回普通池并清除标记（精炼真元已耗尽时同样清除标记）。</li>
 * </ul>
 */

public final class ApertureEssenceService {

    private ApertureEssenceService() {}

    public static final long BASE_REGEN_PER_DAY = 100L;
    public static final int REGEN_INTERVAL_TICKS = Ticks.SECOND;

    private static long getRegenPerDay(@NotNull Aperture aperture) {
        return BASE_REGEN_PER_DAY * aperture.getTalent().getRegenRate() * aperture.rank().getRankBase()
                * aperture.stage().getEssenceMultiplier();
    }

    private static double getRegenPerTick(@NotNull Aperture aperture) {
        return getRegenPerDay(aperture) / (double) Ticks.DAY;
    }

    public static final long DISTILLED_RATE = 2L;
    private static final int NO_SKIP = -1;

    public static long getCurrentEssence(@NotNull Player player) {
        return ApertureService.getAperture(player).currentEssence();
    }

    public static long getMaxEssence(@NotNull Player player) {
        return ApertureService.getAperture(player).getMaxEssence();
    }

    public static long getDistilledEssence(@NotNull Player player) {
        return ApertureService.getAperture(player).distilledEssence();
    }

    private static boolean hasDistillingAperture(@NotNull Player player) {
        ApertureData data = ApertureService.get(player);
        for (int i = 0; i < data.count(); i++) {
            if (data.get(i).distilling()) return true;
        }
        return false;
    }

    public static long getSpendable(@NotNull Player player) {
        ApertureData data = ApertureService.get(player);
        long total = 0L;
        for (int i = 0; i < data.count(); i++) {
            total += data.get(i).currentEssence() + data.get(i).distilledEssence() * DISTILLED_RATE;
        }
        return total;
    }

    public static boolean isDistilling(@NotNull Player player) {
        return PathEffectService.find(player, ModPathEffectTypes.LIQUOR_DISTILLING) != null;
    }

    public static void add(@NotNull ServerPlayer player, long delta) {
        long left = delta;
        ApertureData data = ApertureService.get(player);
        for (int i = 0; i < data.count() && left > 0L; i++) {
            Aperture aperture = data.get(i);
            long room = Math.max(0L, aperture.getMaxEssence() - aperture.currentEssence());
            long given = Math.min(left, room);
            if (given > 0L) set(player, i, aperture.currentEssence() + given);
            left -= given;
        }
    }

    public static void set(@NotNull ServerPlayer player, int index, long value) {
        ApertureService.set(player, index, ApertureService.getAperture(player, index).withCurrentEssence(value));
    }

    public static void refill(@NotNull ServerPlayer player) {
        ApertureData data = ApertureService.get(player);
        for (int i = 0; i < data.count(); i++) {
            ApertureService.set(player, i, data.get(i).withFullEssence());
        }
    }

    public static void refillWithDistilled(@NotNull ServerPlayer player) {
        for (int i = 0; i < ApertureService.get(player).count(); i++) refillWithDistilled(player, i);
    }

    public static void refillWithDistilled(@NotNull ServerPlayer player, int index) {
        Aperture aperture = ApertureService.getAperture(player, index).withFullEssence();
        if (aperture.distilling()) aperture = aperture.withDistilledEssence(aperture.getMaxEssence());
        ApertureService.set(player, index, aperture);
    }

    public static void addDistilled(@NotNull ServerPlayer player, long delta) {
        setDistilled(player, LongMath.saturatedAdd(getDistilledEssence(player), delta));
    }

    private static void setDistilled(@NotNull ServerPlayer player, long value) {
        setDistilled(player, ApertureData.PRIMARY, value);
    }

    private static void setDistilled(@NotNull ServerPlayer player, int index, long value) {
        ApertureService.set(player, index,
                ApertureService.getAperture(player, index).withDistilledEssence(value));
    }

    //region the Liquor Worm's three phases [酒虫三相]
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

    public static void closeDistilling(@NotNull ServerPlayer player) {
        if (hasDistillingAperture(player) && !isDistilling(player)) endDistilling(player);
    }

    private static void endDistilling(@NotNull ServerPlayer player) {
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
        if (getSpendable(player) < amount) return false;

        ApertureData data = ApertureService.get(player);
        takeExcept(player, data, cascadeTake(amount, data.apertures()), NO_SKIP);
        return true;
    }

    public static @Nullable Aperture consumeDeferred(@NotNull ServerPlayer player, long amount, int index) {
        ApertureData data = ApertureService.get(player);
        if (amount <= 0L) return data.get(index);
        if (getSpendable(player) < amount) return null;

        long[][] plan = cascadeTake(amount, data.apertures());
        takeExcept(player, data, plan, index);
        return index < plan.length ? applyTake(data.get(index), plan[index]) : data.get(index);
    }

    private static void takeExcept(ServerPlayer player, ApertureData data, long[][] plan, int skipped) {
        for (int i = 0; i < plan.length; i++) {
            if (i != skipped && (plan[i][0] > 0L || plan[i][1] > 0L)) {
                ApertureService.set(player, i, applyTake(data.get(i), plan[i]));
            }
        }
    }

    private static Aperture applyTake(Aperture aperture, long[] take) {
        return aperture.withDistilledEssence(aperture.distilledEssence() - take[0])
                .withCurrentEssence(aperture.currentEssence() - take[1]);
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

    public static void regenStep(@NotNull ServerPlayer player, @NotNull Beat beat) {
        ApertureData data = ApertureService.get(player);
        float[] carry = PlayerData.get(player).getEssenceCarry();
        for (int index = 0; index < data.count(); index++) {
            regenAperture(player, beat, data.get(index), index, carry);
        }
    }

    private static void regenAperture(
            ServerPlayer player, Beat beat, Aperture aperture, int index, float[] carry) {
        if (ApertureService.getStatus(player, index) != ApertureStatus.NORMAL) {
            carry[index] = 0.0F;
            return;
        }
        boolean distilling = aperture.distilling();
        long current = distilling ? aperture.distilledEssence() : aperture.currentEssence();
        if (current >= aperture.getMaxEssence()) {
            carry[index] = 0.0F;
            return;
        }

        double perStep = beat.scale(getRegenPerTick(aperture) * REGEN_INTERVAL_TICKS);
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
