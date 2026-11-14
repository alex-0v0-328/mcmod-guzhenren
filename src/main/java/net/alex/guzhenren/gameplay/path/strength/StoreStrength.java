package net.alex.guzhenren.gameplay.path.strength;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import net.alex.guzhenren.Guzhenren;
import net.alex.guzhenren.core.Ticks;
import net.alex.guzhenren.registry.effect.ModEffects;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import org.jetbrains.annotations.NotNull;

/**
 * 蓄力蛊[Store Strength Gu]的蓄力与蓄力一拳规则。
 * <ul>
 *   <li>条件：该蛊增益运行（读标记效果，同全力以赴蛊[All-Out Effort Gu]）、持兽力虚影流或人力钧力流、空手、
 *       Epic Fight 锁定时按住攻击键 {@link #HOLD_TICKS}，客户端再请求 {@link #start}。</li>
 *   <li>蓄满需 {@link #CHARGE_TICKS}，三 / 四 / 五转全力以赴下为 70% / 60% / 50%，逐 tick 按当时转数推进；
 *       仅蓄满松手算数（容差 {@link #RELEASE_SLACK_TICKS} 吸收数据包抖动）；未满、丢增益、分支或空手、死亡、换维度、
 *       下线都取消，不出拳不扣费。</li>
 *   <li>本类 tick 是真相：首次判满与自行取消时发 {@link StoreStrengthChargeEvent}，服务端卡顿时客户端不会先亮蓄满；
 *       客户端进度只供蓄力条。</li>
 *   <li>蓄满发挥最多力量，可超当前承受力：兽力虚影流解放兽种（随机一种，苦力下全部）
 *       的永久点数计 {@link #CHARGED_RELEASE_FACTOR} 倍，人力无解放；超出部分为冲击反噬（{@link BodyOverload}）。</li>
 *   <li>近战为脚下指向锁定目标的水平扇形，射程 {@link #getRange} 随最大点数的平方根增长、封顶 {@link #MAX_RANGE}；
 *       远程在目标周围爆开，半径为射程一半；苦力下的人力钧力流射程乘 {@link #HARDSHIP_RANGE_FACTOR}（仍封顶）并加宽
 *       半角；{@link #release} 算数后由 {@code ChargedPunch} 打出。</li>
 *   <li>⚠ 数值全部暂定。</li>
 *   <li>⚠ 只存服务端内存，按玩家索引，不存档。</li>
 * </ul>
 *
 * @author Alex
 * @version 1.0.0
 * @see StoreStrengthChargeEvent
 * @since 1.0.0
 */

@EventBusSubscriber(modid = Guzhenren.MOD_ID)
public final class StoreStrength {

    private StoreStrength() {}

    public static final int CHARGE_TICKS = 4 * Ticks.SECOND;
    private static final int[] ALL_OUT_PERCENT = { 70, 60, 50 };
    public static final int HOLD_TICKS = 5;
    public static final int RELEASE_SLACK_TICKS = 2;
    public static final int CHARGED_RELEASE_FACTOR = 12;
    public static final double BASE_RANGE = 4.0D;
    public static final double RANGE_PER_ROOT_POINT = 0.3D;
    public static final double MAX_RANGE = 16.0D;
    public static final double HARDSHIP_RANGE_FACTOR = 1.5D;
    public static final double HALF_ANGLE_DEGREES = 45.0D;
    public static final double HARDSHIP_HALF_ANGLE_DEGREES = 135.0D;
    public static final double BELOW = 1.0D;
    public static final double ABOVE = 2.0D;
    private static final double MIN_LENGTH_SQR = 1.0E-6D;
    private static final float FILL_EPSILON = 1.0E-4F;
    private static final Map<UUID, Float> CHARGES = new HashMap<>();

    //region Rule [规则]
    public static int getChargeTicks(int allOutTier) {
        if (allOutTier < StrengthService.ALL_OUT_LOWEST_TIER) return CHARGE_TICKS;
        int percent = ALL_OUT_PERCENT[Math.min(allOutTier - StrengthService.ALL_OUT_LOWEST_TIER,
                ALL_OUT_PERCENT.length - 1)];
        return CHARGE_TICKS * percent / 100;
    }

    public static float getStep(int allOutTier) { return 1.0F / getChargeTicks(allOutTier); }

    public static float fill(float progress, int allOutTier) {
        return Math.min(1.0F, progress + getStep(allOutTier));
    }

    public static boolean isFull(float progress, int allOutTier) {
        return progress + RELEASE_SLACK_TICKS * getStep(allOutTier) >= 1.0F - FILL_EPSILON;
    }

    public static int getMaxPoints(int totalPoints, int releasedPermanentPoints) {
        long points = (long) Math.max(0, totalPoints)
                + (long) (CHARGED_RELEASE_FACTOR - 1) * Math.max(0, releasedPermanentPoints);
        return (int) Math.min(Integer.MAX_VALUE, points);
    }

    public static int getBacklashPoints(int maxPoints, int capacity) { return Math.max(0, maxPoints - capacity); }

    public static @NotNull List<BeastStrengthFamily> getReleasedFamilies(@NotNull List<BeastStrengthFamily> available,
                                                                       boolean hardship,
                                                                       @NotNull RandomSource random) {
        if (hardship) return List.copyOf(available);
        return StrengthRound.pickFamilies(available, 1, random);
    }

    public static int getReleasedPoints(@NotNull PathStrengthData data, @NotNull List<BeastStrengthFamily> families) {
        int points = 0;
        for (BeastStrengthFamily family : families) points += Math.max(0, data.getBeastPoints(family));
        return points;
    }

    public static double getRange(int maxPoints, boolean widened) {
        double range = BASE_RANGE + RANGE_PER_ROOT_POINT * Math.sqrt(Math.max(0, maxPoints));
        return Math.min(MAX_RANGE, widened ? range * HARDSHIP_RANGE_FACTOR : range);
    }

    public static double getHalfAngle(boolean widened) {
        return widened ? HARDSHIP_HALF_ANGLE_DEGREES : HALF_ANGLE_DEGREES;
    }

    public static double getBurstRadius(int maxPoints, boolean widened) { return getRange(maxPoints, widened) / 2.0D; }

    public static @NotNull Vec3 getLevelAxis(@NotNull Vec3 toward) {
        Vec3 level = new Vec3(toward.x, 0.0D, toward.z);
        return level.lengthSqr() < MIN_LENGTH_SQR ? Vec3.ZERO : level.normalize();
    }

    public static boolean isInFan(@NotNull Vec3 feet, double height, @NotNull Vec3 axis, double range,
                                  double halfAngleDegrees, @NotNull AABB box) {
        if (box.maxY < feet.y - BELOW || box.minY > feet.y + height + ABOVE) return false;

        Vec3 center = box.getCenter();
        double dx = center.x - feet.x;
        double dz = center.z - feet.z;
        double distance = Math.sqrt(dx * dx + dz * dz);
        double reach = range + Math.max(box.getXsize(), box.getZsize()) / 2.0D;
        if (distance > reach) return false;
        if (distance * distance < MIN_LENGTH_SQR) return true;

        double cos = (dx * axis.x + dz * axis.z) / distance;
        return cos >= Math.cos(Math.toRadians(halfAngleDegrees)) - 1.0E-9D;
    }
    //endregion

    //region Charge [蓄力]
    private static boolean canCharge(@NotNull Player player) {
        if (!player.isAlive() || player.isSpectator() || !AttackDamageService.isFistHand(player)) return false;
        PathStrengthData data = PathStrengthService.get(player);
        return (data.hasPathBranch(StrengthPathBranch.BEAST_STRENGTH_PHANTOM)
                || data.hasPathBranch(StrengthPathBranch.HUMAN_JUN_STRENGTH))
                && player.hasEffect(ModEffects.STORE_STRENGTH_GU);
    }

    public static boolean isCharging(@NotNull Player player) { return CHARGES.containsKey(player.getUUID()); }

    public static float getProgress(@NotNull Player player) {
        Float progress = CHARGES.get(player.getUUID());
        return progress == null ? 0.0F : progress;
    }

    public static boolean start(@NotNull ServerPlayer player) {
        if (!canCharge(player)) return false;
        CHARGES.put(player.getUUID(), 0.0F);
        return true;
    }

    public static void cancel(@NotNull Player player) { CHARGES.remove(player.getUUID()); }

    public static boolean release(@NotNull ServerPlayer player) {
        Float progress = CHARGES.remove(player.getUUID());
        if (progress == null || !isFull(progress, StrengthService.getAllOutTier(player))) return false;
        return canCharge(player);
    }

    private static void drop(@NotNull ServerPlayer player) {
        if (CHARGES.remove(player.getUUID()) != null) {
            NeoForge.EVENT_BUS.post(new StoreStrengthChargeEvent(player, StoreStrengthChargeEvent.Change.DROPPED));
        }
    }
    //endregion

    //region Event [事件]
    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;

        Float progress = CHARGES.get(player.getUUID());
        if (progress == null) return;
        if (!canCharge(player)) {
            drop(player);
            return;
        }
        int tier = StrengthService.getAllOutTier(player);
        float filled = fill(progress, tier);
        CHARGES.put(player.getUUID(), filled);
        if (!isFull(progress, tier) && isFull(filled, tier)) {
            NeoForge.EVENT_BUS.post(new StoreStrengthChargeEvent(player, StoreStrengthChargeEvent.Change.FULL));
        }
    }

    @SubscribeEvent
    public static void onDeath(LivingDeathEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) cancel(player);
    }

    @SubscribeEvent
    public static void onLoggedOut(PlayerEvent.PlayerLoggedOutEvent event) { cancel(event.getEntity()); }

    @SubscribeEvent
    public static void onChangedDimension(PlayerEvent.PlayerChangedDimensionEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) drop(player);
    }

    @SubscribeEvent
    public static void onServerStopped(ServerStoppedEvent event) { CHARGES.clear(); }
    //endregion
}
