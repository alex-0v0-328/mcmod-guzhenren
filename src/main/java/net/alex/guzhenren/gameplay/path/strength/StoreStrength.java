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
 * The Store Strength Gu's [蓄力蛊] charge [蓄力] and the rules of the charged punch [蓄力一拳] it lets go. While the
 * Gu's buff runs (its marker effect, read like the All-Out Effort Gu's), a body holding the beast phantom [兽力虚影流] or the human jun [人力钧力流] branch, a bare main
 * hand ({@link AttackDamageService#isFistHand}) and an Epic Fight lock-on [锁定] holds the attack key: after
 * {@link #HOLD_TICKS} the client asks {@link #start}, and the charge fills by {@link #getStep} a tick, so it is full
 * after {@link #getChargeTicks}: {@link #CHARGE_TICKS}, or 70/60/50% of it while the rank three/four/five All-Out
 * Effort Gu [全力以赴] runs. The step is read every tick, so an All-Out Effort that starts or ends mid-charge speeds or
 * slows only the rest. Letting go ({@link #release}) counts only when the charge is full, give or take
 * {@link #RELEASE_SLACK_TICKS} for the packets' jitter; anything short of that, a lost buff, branch or empty hand, death, a
 * dimension change or logout drops it with no strike and no cost. This tick is the charge's truth: it posts
 * {@link StoreStrengthChargeEvent} the tick the charge first counts as full and when it drops the charge on its own,
 * so a lagging server never shows a client a full charge it does not hold.
 *
 * <p>A full charge brings the most strength to bear, past the current capacity [承受力] ({@link #getMaxPoints}): every
 * point counts once, and on the beast path the families it releases count {@link #CHARGED_RELEASE_FACTOR} times their
 * permanent points -- one family at random ({@link #getReleasedFamilies}), every held family under Hardship Strength
 * [苦力]. The human jun path has no release; its jin count once. The points past the current capacity come back as
 * the impact backlash [冲击反噬] ({@link #getBacklashPoints}), paid through {@link BodyOverload}'s recoil.
 *
 * <p>The melee strike is a level fan [扇形] from the feet along the way to the locked target ({@link #isInFan}):
 * {@link #RANGE} blocks and {@link #HALF_ANGLE_DEGREES} either side, or {@link #HARDSHIP_RANGE} and
 * {@link #HARDSHIP_HALF_ANGLE_DEGREES} for a human jun body under Hardship Strength, from {@link #BELOW} under the
 * feet to {@link #ABOVE} over the head. The ranged strike bursts {@link #BURST_RADIUS} blocks around the target,
 * {@link #HARDSHIP_BURST_RADIUS} under the same widening. The Gu's {@code ChargedPunch} deals both once
 * {@link #release} counts.
 *
 * <p>⚠ Server memory only, keyed by player: never saved. The client fills its own copy of the progress for the bar
 * and waits for the full notice before it shows the charge full. ⚠ Every number here is provisional.
 *
 * @author Alex
 * @version 1.0.0
 * @see StoreStrengthChargeEvent
 * @since 1.0.0
 */

@EventBusSubscriber(modid = Guzhenren.MOD_ID)
public final class StoreStrength {

    private StoreStrength() {}

    public static final int CHARGE_TICKS = 8 * Ticks.SECOND;
    private static final int[] ALL_OUT_PERCENT = { 70, 60, 50 };
    public static final int HOLD_TICKS = 5;
    public static final int RELEASE_SLACK_TICKS = 2;
    public static final int CHARGED_RELEASE_FACTOR = 12;
    public static final double RANGE = 6.0D;
    public static final double HALF_ANGLE_DEGREES = 45.0D;
    public static final double HARDSHIP_RANGE = 9.0D;
    public static final double HARDSHIP_HALF_ANGLE_DEGREES = 135.0D;
    public static final double BELOW = 1.0D;
    public static final double ABOVE = 2.0D;
    public static final double BURST_RADIUS = 3.0D;
    public static final double HARDSHIP_BURST_RADIUS = 4.5D;
    public static final double ARC_STEP_DEGREES = 15.0D;
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

    public static double getRange(boolean widened) { return widened ? HARDSHIP_RANGE : RANGE; }

    public static double getHalfAngle(boolean widened) {
        return widened ? HARDSHIP_HALF_ANGLE_DEGREES : HALF_ANGLE_DEGREES;
    }

    public static double getBurstRadius(boolean widened) { return widened ? HARDSHIP_BURST_RADIUS : BURST_RADIUS; }

    public static int getArcPoints(double halfAngleDegrees) {
        return (int) Math.round(2.0D * halfAngleDegrees / ARC_STEP_DEGREES) + 1;
    }

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
