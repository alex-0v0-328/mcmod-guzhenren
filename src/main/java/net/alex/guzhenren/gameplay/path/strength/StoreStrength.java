package net.alex.guzhenren.gameplay.path.strength;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.alex.guzhenren.Guzhenren;
import net.alex.guzhenren.core.Ticks;
import net.alex.guzhenren.gameplay.data.PlayerData;
import net.alex.guzhenren.registry.effect.ModEffects;
import net.minecraft.server.level.ServerPlayer;
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
 * 蓄力蛊[Store Strength Gu]与惯力蛊[Habitual Strength Gu]的蓄力进度与蓄力一拳[Charged Punch]结算规则。
 * <ul>
 *   <li>起蓄条件（{@code canCharge}）：身上带有蓄力蛊或惯力蛊状态标记（{@link #hasBuff}）、已入兽力虚影流或人力钧力流
 *       （含生效中的限时兽力）、主手为空（{@link AttackDamageService#isFistHand}）；客户端在 Epic Fight 锁定目标并按住
 *       攻击键满 {@link #HOLD_TICKS} 后发包请求 {@link #start}。</li>
 *   <li>起蓄瞬间若带有惯力蛊状态，则开启可移动蓄力标记（{@link #isMobile}，双蛊并存时按惯力蛊计，同步给所有追踪客户端并
 *       在蓄力结束或中断时撤除）；可移动蓄力期间不能疾跑，服务端每 tick 清疾跑标记，客户端另松开疾跑键。</li>
 *   <li>基础蓄满时长为 {@link #CHARGE_TICKS}（三、四、五转全力以赴蛊[All-Out Effort Gu]运行期间分别缩短至 70%、60%、
 *       50%，每 tick 按当前档位步进）；进度达到 {@link #MIN_PROGRESS}（基础 1 秒）后松手才出拳（{@link #isReady}，留有
 *       {@link #RELEASE_SLACK_TICKS} 容差吸收网络抖动），更早松手、中途失去增益/分支/空手、死亡、切换维度或登出均取消
 *       蓄力，不出拳也不扣费。</li>
 *   <li>蓄力倍率 {@link #getFactor} 随进度线性增长：{@code 1 + (MAX_FACTOR - 1) × 进度}，最短蓄力时为 1.5，蓄满为
 *       {@link #MAX_FACTOR}；作为整拳倍率进入结算力量点[Strength Points]（进入前由 {@link #getExactFactor} 取整到万分
 *       位，消掉逐 tick 累加的浮点误差）。</li>
 *   <li>服务端 tick 为唯一权威：首次蓄满时发布 {@link StoreStrengthChargeEvent.Change#FULL}，服务端被动中断时发布
 *       {@link StoreStrengthChargeEvent.Change#DROPPED}，避免服务端卡顿时客户端抢先亮起蓄满提示。</li>
 *   <li>兽力虚影流的蓄力一拳单独掷骰（{@link #getChance}：{@link #CHANCE} 加兽力加成，全力以赴运行中为 100%），掷中即
 *       全面解放全部永久兽种，与苦力和连击数无关，也不计入连击数。</li>
 *   <li>打击范围：近战为从脚底指向锁定目标的水平扇形（射程 {@link #getRange} 随结算力量点的平方根增长并封顶于
 *       {@link #MAX_RANGE}），远程气波在锁定目标处爆开（半径为射程的一半）；人力钧力流在苦力状态下
 *       （{@link AreaImpact#isWidened}）射程乘 {@link AreaImpact#WIDENED_RANGE_FACTOR} 并拓宽扇形半角。</li>
 *   <li>⚠ 蓄力进度仅保存在服务端内存映射 {@code CHARGES} 中，不落盘存储。</li>
 * </ul>
 */

@EventBusSubscriber(modid = Guzhenren.MOD_ID)
public final class StoreStrength {

    private StoreStrength() {}

    public static final int CHARGE_TICKS = 4 * Ticks.SECOND;
    private static final int[] ALL_OUT_PERCENT = { 70, 60, 50 };
    public static final int HOLD_TICKS = 5;
    public static final int RELEASE_SLACK_TICKS = 2;
    public static final float MIN_PROGRESS = 0.25F;
    public static final float MAX_FACTOR = 3.0F;
    public static final float CHANCE = 0.7F;
    private static final double FACTOR_STEPS = 10_000.0D;
    public static final double BASE_RANGE = 4.0D;
    public static final double RANGE_PER_ROOT_POINT = 0.3D;
    public static final double MAX_RANGE = 16.0D;
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

    public static boolean isReady(float progress, int allOutTier) {
        return progress + RELEASE_SLACK_TICKS * getStep(allOutTier) >= MIN_PROGRESS - FILL_EPSILON;
    }

    public static float getFactor(float progress) {
        return 1.0F + (MAX_FACTOR - 1.0F) * Math.clamp(progress, 0.0F, 1.0F);
    }

    public static float getFactor(float progress, int allOutTier) {
        if (!isReady(progress, allOutTier)) return 0.0F;
        return getFactor(isFull(progress, allOutTier) ? 1.0F : Math.max(MIN_PROGRESS, progress));
    }

    public static double getExactFactor(float factor) { return Math.round(factor * FACTOR_STEPS) / FACTOR_STEPS; }

    public static float getChance(boolean allOut, float beastBonus) {
        return allOut ? 1.0F : Math.min(1.0F, CHANCE + beastBonus);
    }

    public static double getRange(int points, boolean widened) {
        double range = Math.min(MAX_RANGE, BASE_RANGE + RANGE_PER_ROOT_POINT * Math.sqrt(Math.max(0, points)));
        return widened ? range * AreaImpact.WIDENED_RANGE_FACTOR : range;
    }

    public static double getBurstRadius(int points, boolean widened) { return getRange(points, widened) / 2.0D; }

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
    public static boolean hasBuff(@NotNull Player player) {
        return player.hasEffect(ModEffects.STORE_STRENGTH_GU) || player.hasEffect(ModEffects.HABITUAL_STRENGTH_GU);
    }

    private static boolean canCharge(@NotNull Player player) {
        if (!player.isAlive() || player.isSpectator() || !AttackDamageService.isFistHand(player)) return false;
        return PathStrengthService.getHeldBranch(player) != StrengthPathBranch.NORMAL && hasBuff(player);
    }

    public static boolean isMobile(@NotNull Player player) { return PlayerData.get(player).isInMobileCharge(); }

    public static boolean isCharging(@NotNull Player player) { return CHARGES.containsKey(player.getUUID()); }

    public static float getProgress(@NotNull Player player) {
        Float progress = CHARGES.get(player.getUUID());
        return progress == null ? 0.0F : progress;
    }

    public static boolean start(@NotNull ServerPlayer player) {
        if (!canCharge(player)) return false;
        CHARGES.put(player.getUUID(), 0.0F);
        PlayerData.get(player).setMobileCharge(player.hasEffect(ModEffects.HABITUAL_STRENGTH_GU));
        return true;
    }

    public static void cancel(@NotNull Player player) {
        CHARGES.remove(player.getUUID());
        PlayerData.get(player).setMobileCharge(false);
    }

    public static float release(@NotNull ServerPlayer player) {
        Float progress = CHARGES.remove(player.getUUID());
        PlayerData.get(player).setMobileCharge(false);
        if (progress == null || !canCharge(player)) return 0.0F;
        return getFactor(progress, StrengthService.getAllOutTier(player));
    }

    private static void drop(@NotNull ServerPlayer player) {
        if (CHARGES.remove(player.getUUID()) != null) {
            NeoForge.EVENT_BUS.post(new StoreStrengthChargeEvent(player, StoreStrengthChargeEvent.Change.DROPPED));
            PlayerData.get(player).setMobileCharge(false);
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
        if (isMobile(player) && player.isSprinting()) player.setSprinting(false);
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
