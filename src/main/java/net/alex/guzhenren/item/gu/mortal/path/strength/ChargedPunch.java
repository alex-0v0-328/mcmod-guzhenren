package net.alex.guzhenren.item.gu.mortal.path.strength;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import net.alex.guzhenren.entity.beast.BeastPhantomEntity;
import net.alex.guzhenren.entity.projectile.BeastChargeEntity;
import net.alex.guzhenren.entity.projectile.QiBurst;
import net.alex.guzhenren.entity.projectile.QiWaveEntity;
import net.alex.guzhenren.gameplay.path.strength.AreaDamage;
import net.alex.guzhenren.gameplay.path.strength.AreaDamageShatter;
import net.alex.guzhenren.gameplay.path.strength.AttackDamageService;
import net.alex.guzhenren.gameplay.path.strength.BeastChargeDamage;
import net.alex.guzhenren.gameplay.path.strength.BeastStrengthFamily;
import net.alex.guzhenren.gameplay.path.strength.BodyOverload;
import net.alex.guzhenren.gameplay.path.strength.ComboRound;
import net.alex.guzhenren.gameplay.path.strength.HardshipStrength;
import net.alex.guzhenren.gameplay.path.strength.PathStrengthData;
import net.alex.guzhenren.gameplay.path.strength.PathStrengthService;
import net.alex.guzhenren.gameplay.path.strength.StoreStrength;
import net.alex.guzhenren.gameplay.path.strength.StrengthCrit;
import net.alex.guzhenren.gameplay.path.strength.StrengthPathBranch;
import net.alex.guzhenren.gameplay.path.strength.StrengthService;
import net.alex.guzhenren.gameplay.path.strength.StrengthSnapshot;
import net.alex.guzhenren.item.gu.mortal.path.qi.QiWaveService;
import net.alex.guzhenren.particle.RingConeEmitter;
import net.alex.guzhenren.registry.damage.ModDamageTypes;
import net.alex.guzhenren.registry.effect.ModEffects;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * 蓄力蛊[Store Strength Gu]与惯力蛊[Habitual Strength Gu]（{@link StoreStrengthBehavior}）松手后释放的蓄力一拳结算工
 * 具。
 * <ul>
 *   <li>由 {@link StoreStrength#release} 确认达到最短蓄力并清槽后触发，蓄力倍率作为伤害倍率进入结算力量点[Strength
 *       Points]（{@link StrengthSnapshot#settle}），拳伤按该点数重算
 *       （{@link AttackDamageService#getFistDamage(net.minecraft.world.entity.player.Player, int)}）。</li>
 *   <li>暴击[Crit]单独掷一次，不进连招轮：蓄满必暴击，未蓄满按 {@link StrengthCrit#getChance} 掷
 *       （{@link StoreStrength#getCritChance}）。兽力虚影流派暴击时从持有的永久兽种中抽 {@link StrengthCrit#getCount}
 *       种（{@code 1 + 苦力档位}），其永久点数再计一份，虚影并入连招轮[Combo Round]收尾仍在显示的身旁虚影一同显形；人力
 *       钧力流派暴击不另加效果（扇形溢出本来就始终有）。{@link #strike(ServerPlayer, LivingEntity, float, boolean)} 接
 *       收掷定的结果，供测试直接调用。</li>
 *   <li>{@link #getTarget} 复验客户端上报的目标实体 ID（须存活、可被命中、位于视野前方且距眼部不超过
 *       {@link #MAX_TARGET_DISTANCE} 格），防止伪造包将拳劲转向身后或超远目标；注意 {@link #MAX_TARGET_DISTANCE} 高于
 *       Epic Fight 默认锁定距离。</li>
 *   <li>当玩家带有力气蛊[Strength Qi Gu]效果、复验目标位于原版近战触及距离外且视线无遮挡、可被弹射物命中时，走远程攻击
 *       （{@link #shoot}），总伤害为拳伤 × {@link QiWaveService#RANGED_FACTOR}：每个可显形的暴击兽种沿弧线发射一道
 *       {@link BeastChargeEntity}，总伤害按兽种份额分摊（{@link BeastChargeDamage}）；暴击兽种均不可显形或没暴击时按
 *       {@link QiWaveService#getChargeFamilies} 从持有兽种中随机一种带全部伤害发射；人力钧力流派或无可显形兽种时改为发
 *       射一枚携带全部伤害的追踪气波（{@link QiWaveEntity#track}）。落点按 {@link StoreStrength#getBurstRadius} 爆开并
 *       按溢出结算，不破坏方块。</li>
 *   <li>不满足远程条件时走近战扇形挥拳（{@link #swing}）：水平轴朝向复验通过的锁定目标（无目标则取视线水平投影），射程
 *       由结算力量点与苦力档位决定（{@link StoreStrength#getRange}，人力流按 {@link AreaDamage#getWidenTier} 放大、封顶
 *       16 格，半角 {@link AreaDamage#getHalfAngle}），对扇形内视线可见的可命中目标按溢出结算
 *       （{@link AreaDamage#spill}：锁定目标排首位吃整拳，剩余由近到远至多 {@link #MAX_TARGETS} 个），各沿脚下连线击退
 *       {@link #KNOCKBACK}；随后在未放大的半数射程内崩裂地形方块（{@link AreaDamageShatter}），并始终沿扇形中轴发射一道
 *       直达射程末端的冲击环（{@link RingConeEmitter#areaDamageRing}，挥空同样播放）。</li>
 *   <li>过载反噬[Body Overload]只在打中时结算一次：近战扇形命中后直接结算，远程交给 {@link BodyOverload.Shot} 在首次造
 *       成伤害的爆开时结算；挥空不扣。</li>
 * </ul>
 */

public final class ChargedPunch {

    private ChargedPunch() {}

    public static final int MAX_TARGETS = 8;
    public static final double KNOCKBACK = 1.0D;
    public static final double MAX_TARGET_DISTANCE = 32.0D;

    public static boolean release(@NotNull ServerPlayer player, int targetId) {
        float factor = StoreStrength.release(player);
        if (factor <= 0.0F) return false;

        strike(player, getTarget(player, targetId), factor);
        return true;
    }

    public static @Nullable LivingEntity getTarget(@NotNull ServerPlayer player, int targetId) {
        if (!(player.level().getEntity(targetId) instanceof LivingEntity target)) return null;
        if (!AreaDamage.canHit(player, target)) return null;

        Vec3 toTarget = target.getBoundingBox().getCenter().subtract(player.getEyePosition());
        if (toTarget.dot(player.getLookAngle()) <= 0.0D) return null;
        return toTarget.lengthSqr() <= MAX_TARGET_DISTANCE * MAX_TARGET_DISTANCE ? target : null;
    }

    public static void strike(@NotNull ServerPlayer player, @Nullable LivingEntity target, float chargeFactor) {
        boolean full = chargeFactor >= StoreStrength.MAX_FACTOR;
        strike(player, target, chargeFactor, player.getRandom().nextFloat()
                < StoreStrength.getCritChance(full, StrengthCrit.getChance(player)));
    }

    public static void strike(@NotNull ServerPlayer player, @Nullable LivingEntity target, float chargeFactor,
                              boolean crit) {
        StrengthSnapshot snapshot = StrengthService.snapshot(player);
        PathStrengthData data = PathStrengthService.get(player);
        boolean beastPath = PathStrengthService.getHeldBranch(player) == StrengthPathBranch.BEAST_STRENGTH_PHANTOM;
        List<BeastStrengthFamily> held = beastPath ? StrengthCrit.getAvailableFamilies(data) : List.of();
        List<BeastStrengthFamily> critFamilies = crit ? StrengthCrit.pickFamilies(held,
                StrengthCrit.getCount(HardshipStrength.getTier(player), held.size()), player.getRandom()) : List.of();
        int points = snapshot.settle(StrengthCrit.getCritPoints(critFamilies, data),
                StoreStrength.getExactFactor(chargeFactor));
        float damage = (float) AttackDamageService.getFistDamage(player, points);
        int widenTier = AreaDamage.getWidenTier(player);

        if (!critFamilies.isEmpty()) BeastPhantomEntity.show(player, getShownFamilies(player, critFamilies));
        if (target != null && isRanged(player, target)) {
            List<BeastStrengthFamily> charging = QiWaveService.getChargeFamilies(beastPath, critFamilies, held,
                    player.getRandom());
            QiBurst.Spec burst = new QiBurst.Spec(StoreStrength.getBurstRadius(points, widenTier),
                    ModDamageTypes.CHARGED_PUNCH, false, new BodyOverload.Shot(points, snapshot.capacity()));
            shoot(player, target, damage * QiWaveService.RANGED_FACTOR, charging,
                    BeastChargeDamage.getWeights(data, charging, critFamilies), burst);
        } else if (swing(player, target, damage, points, widenTier)) {
            BodyOverload.recoil(player, points, snapshot.capacity());
        }
    }

    private static boolean isRanged(@NotNull ServerPlayer player, @NotNull LivingEntity target) {
        return player.hasEffect(ModEffects.STRENGTH_QI_GU) && target.canBeHitByProjectile()
                && QiWaveService.isPastReach(QiWaveService.getBoxDistance(player, target),
                player.entityInteractionRange())
                && player.hasLineOfSight(target);
    }

    private static List<BeastStrengthFamily> getShownFamilies(ServerPlayer player, List<BeastStrengthFamily> crit) {
        List<BeastStrengthFamily> shown = new ArrayList<>(ComboRound.getShown(player));
        for (BeastStrengthFamily family : crit) {
            if (!shown.contains(family)) shown.add(family);
        }
        return shown;
    }

    //region Ranged [远程]
    private static void shoot(ServerPlayer player, LivingEntity target, float damage,
                              List<BeastStrengthFamily> charging, int[] weights, QiBurst.Spec burst) {
        if (charging.isEmpty()) {
            QiWaveEntity.track(player, target, damage, burst);
            return;
        }
        float[] damages = BeastChargeDamage.split(damage, weights);
        for (int i = 0; i < charging.size(); i++) {
            BeastChargeEntity.shoot(player, target, damages[i], charging.get(i), burst, i, charging.size());
        }
    }
    //endregion

    //region Melee [近战]
    private static boolean swing(ServerPlayer player, @Nullable LivingEntity locked, float damage, int points,
                                 int widenTier) {
        Vec3 feet = player.position();
        Vec3 aimed = StoreStrength.getLevelAxis(locked != null ? locked.position().subtract(feet)
                : player.getLookAngle());
        Vec3 axis = aimed == Vec3.ZERO
                ? StoreStrength.getLevelAxis(Vec3.directionFromRotation(0.0F, player.getYRot())) : aimed;
        double range = StoreStrength.getRange(points, widenTier);
        double halfAngle = AreaDamage.getHalfAngle(widenTier);

        List<LivingEntity> targets = new ArrayList<>(player.level().getEntitiesOfClass(LivingEntity.class,
                player.getBoundingBox().inflate(range + 2.0D, StoreStrength.ABOVE + StoreStrength.BELOW,
                        range + 2.0D),
                target -> AreaDamage.canHit(player, target)
                        && StoreStrength.isInFan(feet, player.getBbHeight(), axis, range, halfAngle,
                        target.getBoundingBox())
                        && player.hasLineOfSight(target)));
        targets.sort(Comparator.comparingDouble((LivingEntity target) -> target == locked ? -1.0D
                : target.distanceToSqr(feet)));

        List<LivingEntity> landed = AreaDamage.spill(targets, damage, MAX_TARGETS,
                ModDamageTypes.source(player, player, ModDamageTypes.CHARGED_PUNCH), target -> {
                    Vec3 away = StoreStrength.getLevelAxis(target.position().subtract(feet));
                    Vec3 push = away == Vec3.ZERO ? axis : away;
                    target.knockback(KNOCKBACK, -push.x, -push.z);
                    target.hurtMarked = true;
                });
        player.swing(InteractionHand.MAIN_HAND, true);
        if (AreaDamageShatter.canShatter(points)) {
            Vec3 eye = player.getEyePosition();
            AreaDamageShatter.shatter(player, eye, AreaDamageShatter.getFanCells(feet, player.getBbHeight(), axis,
                    StoreStrength.getRange(points, 0) / 2.0D, AreaDamage.HALF_ANGLE_DEGREES, eye), points,
                    AreaDamageShatter.FAN_MAX_BLOCKS);
        }
        showSwing(player, axis, range);
        return !landed.isEmpty();
    }

    private static void showSwing(ServerPlayer player, Vec3 axis, double range) {
        ServerLevel level = player.serverLevel();
        RingConeEmitter.areaDamageRing(level, player.position().add(0.0D, player.getBbHeight() * 0.5D, 0.0D), axis,
                range);
        level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.PLAYER_ATTACK_SWEEP,
                SoundSource.PLAYERS, 1.0F, 0.6F);
        level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.PLAYER_ATTACK_KNOCKBACK,
                SoundSource.PLAYERS, 1.0F, 0.8F);
    }
    //endregion
}
