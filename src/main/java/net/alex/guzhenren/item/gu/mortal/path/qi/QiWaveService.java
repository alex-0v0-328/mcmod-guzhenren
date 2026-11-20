package net.alex.guzhenren.item.gu.mortal.path.qi;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import net.alex.guzhenren.Guzhenren;
import net.alex.guzhenren.effect.timed.StrengthQiGuEffect;
import net.alex.guzhenren.entity.projectile.BeastChargeEntity;
import net.alex.guzhenren.entity.projectile.QiBurst;
import net.alex.guzhenren.entity.projectile.QiWaveEntity;
import net.alex.guzhenren.gameplay.path.strength.AreaDamage;
import net.alex.guzhenren.gameplay.path.strength.AttackDamageService;
import net.alex.guzhenren.gameplay.path.strength.BeastChargeDamage;
import net.alex.guzhenren.gameplay.path.strength.BeastStrengthFamily;
import net.alex.guzhenren.gameplay.path.strength.BodyOverload;
import net.alex.guzhenren.gameplay.path.strength.ComboRound;
import net.alex.guzhenren.gameplay.path.strength.PathStrengthData;
import net.alex.guzhenren.gameplay.path.strength.PathStrengthService;
import net.alex.guzhenren.gameplay.path.strength.StrengthCrit;
import net.alex.guzhenren.gameplay.path.strength.StrengthPathBranch;
import net.alex.guzhenren.gameplay.path.strength.StrengthService;
import net.alex.guzhenren.gameplay.path.strength.StrengthSnapshot;
import net.alex.guzhenren.item.gu.mortal.behavior.TimedEffects;
import net.alex.guzhenren.registry.damage.ModDamageTypes;
import net.alex.guzhenren.registry.effect.ModEffects;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * 力气蛊[Strength Qi Gu]（{@link StrengthQiGuEffect}）远程攻击的服务端发射服务；力气蛊只管远程攻击，不再自带范围伤害
 * 概率。
 * <ul>
 *   <li>处于力气蛊增益期间，Epic Fight 战斗模式下空手出拳且锁定目标在原版近战触及距离外时，这一拳化作远程攻击（客户端
 *       仅上报目标实体 ID，服务端全量复验，射程仅受原版 128 格视线限制）；{@link #isPastReach} 是双端共用的唯一射程分
 *       界，触及距离内留给近战，同一拳不会近战与远程双重命中。</li>
 *   <li>一拳一发、没有间隔：客户端按下攻击键的上报（{@link #request}）与服务端 Epic Fight 真正起招（{@link #onStrike}）
 *       在 {@link #JOIN_TICKS} 内配对才发射，先到的一方等待另一方；Epic Fight 拒绝出招（动作中、采矿模式等）时上报作废，
 *       连按不会多发。</li>
 *   <li>远程攻击的总伤害 = 当次空手拳伤（按出手时的结算力量点[Strength Points]重算，收尾招含暴击）×
 *       {@link #RANGED_FACTOR} 远程系数，不随原版攻击蓄力条缩放；远程系数不影响过载反噬，结算力量点与承受力在出手时记入
 *       {@link BodyOverload.Shot} 并登记到本招（{@link ComboRound#markShot}），弹射物爆开或同一招的近战命中首次造成
 *       伤害时结算一次。</li>
 *   <li>只有连招轮[Combo Round]的收尾招结算本轮暴击[Crit]（{@link ComboRound#strike}），并通知
 *       {@link ComboRound#markRanged} 不再放近战锥形：人力钧力流暴击（{@link ComboRound#rollsArea}）时爆开半径取范围伤害
 *       [Area Damage]半径（{@link AreaDamage#getRange(int, int)}，含苦力放大），否则为 {@link QiBurst#RADIUS}；兽力虚影
 *       流恒为 {@link QiBurst#RADIUS}，按本招暴击抽中的兽种各发一道 {@link BeastChargeEntity}，总伤害按兽种份额分摊
 *       （{@link BeastChargeDamage}）。前几段或本招没抽中时从已持有的可显形永久兽力中随机一种带全部伤害发射。不可显形的
 *       兽种（{@code BeastStrengthFamily#canManifest}）不发射冲击。</li>
 *   <li>其他流派或未持有可显形永久兽力的玩家改为发射一枚带全部伤害的 {@link QiWaveEntity}。</li>
 * </ul>
 */

@EventBusSubscriber(modid = Guzhenren.MOD_ID)
public final class QiWaveService {

    private QiWaveService() {}

    public static final int JOIN_TICKS = 2;
    public static final float RANGED_FACTOR = 0.5F;
    private static final Map<UUID, Shot> SHOTS = new HashMap<>();
    private static final Map<UUID, Strike> STRIKES = new HashMap<>();

    private record Shot(int targetId, long tick) {}

    private record Strike(boolean finishing, long tick) {}

    public static boolean isPastReach(double distance, double reach) {
        return distance > reach;
    }

    public static boolean canPunch(@NotNull Player player) {
        return player.getMainHandItem().isEmpty() && player.isAlive() && !player.isSpectator();
    }

    public static double getBoxDistance(@NotNull Player player, @NotNull LivingEntity target) {
        return Math.sqrt(target.getBoundingBox().distanceToSqr(player.getEyePosition()));
    }

    public static boolean isJoined(long from, long now) { return now >= from && now - from <= JOIN_TICKS; }

    public static @NotNull List<BeastStrengthFamily> getChargeFamilies(boolean beastPath,
                                                                       @NotNull List<BeastStrengthFamily> drawn,
                                                                       @NotNull List<BeastStrengthFamily> held,
                                                                       @NotNull RandomSource random) {
        List<BeastStrengthFamily> shown = BeastStrengthFamily.getManifesting(drawn);
        List<BeastStrengthFamily> pool = BeastStrengthFamily.getManifesting(held);
        if (!beastPath || pool.isEmpty()) return List.of();
        if (!shown.isEmpty()) return List.copyOf(shown);
        return List.of(pool.get(random.nextInt(pool.size())));
    }

    //region Pairing [配对]
    public static @NotNull List<Projectile> request(@NotNull ServerPlayer player, int targetId) {
        if (getTarget(player, targetId) == null) return List.of();

        long now = player.level().getGameTime();
        Strike strike = STRIKES.remove(player.getUUID());
        if (strike != null && isJoined(strike.tick(), now)) return fire(player, targetId, strike.finishing());
        SHOTS.put(player.getUUID(), new Shot(targetId, now));
        return List.of();
    }

    public static @NotNull List<Projectile> onStrike(@NotNull ServerPlayer player, boolean finishing) {
        long now = player.level().getGameTime();
        Shot shot = SHOTS.remove(player.getUUID());
        if (shot != null && isJoined(shot.tick(), now)) return fire(player, shot.targetId(), finishing);
        STRIKES.put(player.getUUID(), new Strike(finishing, now));
        return List.of();
    }

    private static void clear(@NotNull Player player) {
        SHOTS.remove(player.getUUID());
        STRIKES.remove(player.getUUID());
    }
    //endregion

    //region Fire [发射]
    public static @Nullable LivingEntity getTarget(@NotNull ServerPlayer player, int targetId) {
        if (!canPunch(player) || !TimedEffects.hasBuff(player, ModEffects.STRENGTH_QI_GU)) return null;
        if (!(player.level().getEntity(targetId) instanceof LivingEntity target) || target == player) return null;
        if (!target.isAlive() || !target.canBeHitByProjectile()) return null;

        if (!isPastReach(getBoxDistance(player, target), player.entityInteractionRange())) return null;
        Vec3 toTarget = target.getBoundingBox().getCenter().subtract(player.getEyePosition());
        if (toTarget.dot(player.getLookAngle()) <= 0.0D) return null;
        return player.hasLineOfSight(target) ? target : null;
    }

    public static @NotNull List<Projectile> fire(@NotNull ServerPlayer player, int targetId, boolean finishing) {
        LivingEntity target = getTarget(player, targetId);
        if (target == null) return List.of();

        if (finishing) ComboRound.strike(player, player.getRandom().nextFloat());
        boolean area = finishing && ComboRound.rollsArea(player);
        if (finishing) ComboRound.markRanged(player);

        StrengthSnapshot snapshot = StrengthService.snapshot(player);
        int points = StrengthService.getSettledPoints(player, snapshot);
        double radius = area ? AreaDamage.getRange(points, AreaDamage.getWidenTier(player)) : QiBurst.RADIUS;
        BodyOverload.Shot shot = new BodyOverload.Shot(points, snapshot.capacity());
        ComboRound.markShot(player, shot);
        QiBurst.Spec burst = new QiBurst.Spec(radius, ModDamageTypes.QI_WAVE, area, shot);
        float damage = (float) AttackDamageService.getFistDamage(player, points) * RANGED_FACTOR;

        PathStrengthData data = PathStrengthService.get(player);
        List<BeastStrengthFamily> drawn = ComboRound.getFamilies(player);
        List<BeastStrengthFamily> families = getChargeFamilies(
                PathStrengthService.getHeldBranch(player) == StrengthPathBranch.BEAST_STRENGTH_PHANTOM, drawn,
                StrengthCrit.getAvailableFamilies(data), player.getRandom());
        if (families.isEmpty()) return List.of(QiWaveEntity.shoot(player, target, damage, burst));

        float[] damages = BeastChargeDamage.split(damage, BeastChargeDamage.getWeights(data, families, drawn));
        List<Projectile> charges = new ArrayList<>();
        for (int i = 0; i < families.size(); i++) {
            charges.add(BeastChargeEntity.shoot(player, target, damages[i], families.get(i), burst, i,
                    families.size()));
        }
        return charges;
    }
    //endregion

    //region Event [事件]
    @SubscribeEvent
    public static void onLoggedOut(PlayerEvent.PlayerLoggedOutEvent event) { clear(event.getEntity()); }

    @SubscribeEvent
    public static void onServerStopped(ServerStoppedEvent event) {
        SHOTS.clear();
        STRIKES.clear();
    }
    //endregion
}
