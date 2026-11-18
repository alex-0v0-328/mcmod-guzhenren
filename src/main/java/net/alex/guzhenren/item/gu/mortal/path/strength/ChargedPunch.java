package net.alex.guzhenren.item.gu.mortal.path.strength;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import net.alex.guzhenren.entity.beast.BeastPhantomEntity;
import net.alex.guzhenren.entity.projectile.BeastChargeEntity;
import net.alex.guzhenren.entity.projectile.QiBurst;
import net.alex.guzhenren.entity.projectile.QiWaveEntity;
import net.alex.guzhenren.gameplay.path.strength.AreaImpact;
import net.alex.guzhenren.gameplay.path.strength.AreaImpactShatter;
import net.alex.guzhenren.gameplay.path.strength.AttackDamageService;
import net.alex.guzhenren.gameplay.path.strength.BeastChargeDamage;
import net.alex.guzhenren.gameplay.path.strength.BeastStrengthFamily;
import net.alex.guzhenren.gameplay.path.strength.BodyOverload;
import net.alex.guzhenren.gameplay.path.strength.HardshipStrength;
import net.alex.guzhenren.gameplay.path.strength.PathStrengthData;
import net.alex.guzhenren.gameplay.path.strength.PathStrengthService;
import net.alex.guzhenren.gameplay.path.strength.StoreStrength;
import net.alex.guzhenren.gameplay.path.strength.StrengthPathBranch;
import net.alex.guzhenren.gameplay.path.strength.StrengthRound;
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
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * 蓄力蛊[Store Strength Gu]与惯力蛊[Habitual Strength Gu]（{@link StoreStrengthBehavior}）蓄力满槽后释放的蓄力一拳结算
 * 工具。
 * <ul>
 *   <li>由 {@link StoreStrength#release} 确认蓄满并清槽后触发；按 {@link StoreStrength#getMaxPoints} 计算出拳总点数，允
 *       许突破当前肉身承受力上限
 *       （{@link AttackDamageService#getFistDamage(net.minecraft.world.entity.player.Player, int)}）。</li>
 *   <li>兽力虚影流派随机解放一种已持有的永久兽力（苦力蛊[Hardship Strength Gu]生效期间则解放全部持有兽种），并将解放的
 *       兽种虚影并入当前攻击轮次的身旁虚影一同显形。</li>
 *   <li>{@link #getTarget} 复验客户端上报的目标实体 ID（须存活、可被命中、位于视野前方且距眼部不超过
 *       {@link #MAX_TARGET_DISTANCE} 格），防止伪造包将拳劲转向身后或超远目标；注意 {@link #MAX_TARGET_DISTANCE} 高于
 *       Epic Fight 默认锁定距离。</li>
 *   <li>当玩家带有力气蛊[Strength Qi Gu]效果、复验目标位于原版近战触及距离外且视线无遮挡、可被弹射物命中时，走远程出手
 *       （{@link #shoot}）：每个可显形的解放兽种沿弧线发射一道 {@link BeastChargeEntity}，若解放兽种均不可显形则按
 *       {@link QiWaveService#getChargeFamilies} 从持有兽种中随机抽取一种可显形兽种发射；人力钧力流派或无可显形兽种时改
 *       为发射一枚携带全额拳伤的追踪气波（{@link QiWaveEntity#track}）。</li>
 *   <li>远程兽力冲击按各自兽种力量计算伤害（{@link BeastChargeDamage}）：本次解放的兽种取永久力量点[Strength Points]乘
 *       以 {@link StoreStrength#CHARGED_RELEASE_FACTOR}，随机兜底的兽种乘 1，且均不截断至肉身承受力；落点按最大点数决定
 *       的 {@link StoreStrength#getBurstRadius} 爆开，锁定目标承受全额伤害、其余目标乘以
 *       {@link QiBurst#SECONDARY_RATIO}，不触发范围冲击且不破坏方块。</li>
 *   <li>不满足远程条件时走近战扇形挥拳（{@link #swing}）：水平轴朝向复验通过的锁定目标（无目标则取视线水平投影），射程
 *       由最大点数决定（{@link StoreStrength#getRange}），对扇形内视线可见的可命中目标由近到远至多结算
 *       {@link #MAX_TARGETS} 个（锁定目标固定排首位并承受全额伤害，其余目标乘以 {@link #SECONDARY_RATIO}），各沿脚下连
 *       线击退 {@link #KNOCKBACK}；随后在半数射程内崩裂地形方块（{@link AreaImpactShatter}），并始终沿扇形中轴发射一道
 *       直达射程末端的冲击环（{@link RingConeEmitter#areaImpactRing}，挥空同样播放）。</li>
 *   <li>无论近战、远程或挥空，出拳后均结算冲击反噬：超出当前肉身承受力的点数经 {@link BodyOverload#recoil} 造成反噬伤害
 *       （该伤害类型不触发新攻击轮次、也不重复计入肉体过载）。</li>
 * </ul>
 */

public final class ChargedPunch {

    private ChargedPunch() {}

    public static final float SECONDARY_RATIO = 0.5F;
    public static final int MAX_TARGETS = 8;
    public static final double KNOCKBACK = 1.0D;
    public static final double MAX_TARGET_DISTANCE = 32.0D;

    public static boolean release(@NotNull ServerPlayer player, int targetId) {
        if (!StoreStrength.release(player)) return false;

        strike(player, getTarget(player, targetId));
        return true;
    }

    public static @Nullable LivingEntity getTarget(@NotNull ServerPlayer player, int targetId) {
        if (!(player.level().getEntity(targetId) instanceof LivingEntity target)) return null;
        if (!AreaImpact.canHit(player, target)) return null;

        Vec3 toTarget = target.getBoundingBox().getCenter().subtract(player.getEyePosition());
        if (toTarget.dot(player.getLookAngle()) <= 0.0D) return null;
        return toTarget.lengthSqr() <= MAX_TARGET_DISTANCE * MAX_TARGET_DISTANCE ? target : null;
    }

    public static void strike(@NotNull ServerPlayer player, @Nullable LivingEntity target) {
        StrengthSnapshot snapshot = StrengthService.snapshot(player);
        PathStrengthData data = PathStrengthService.get(player);
        StrengthPathBranch branch = PathStrengthService.getHeldBranch(player);
        boolean hardship = HardshipStrength.isActive(player);
        List<BeastStrengthFamily> released = branch == StrengthPathBranch.BEAST_STRENGTH_PHANTOM
                ? StoreStrength.getReleasedFamilies(StrengthRound.getAvailableFamilies(data), hardship,
                player.getRandom())
                : List.of();
        int maxPoints = StoreStrength.getMaxPoints(snapshot.totalPoints(),
                StoreStrength.getReleasedPoints(data, released));
        float damage = (float) AttackDamageService.getFistDamage(player, maxPoints);
        boolean widened = hardship && branch == StrengthPathBranch.HUMAN_JUN_STRENGTH;

        if (!released.isEmpty()) BeastPhantomEntity.show(player, getShownFamilies(player, released));
        if (target != null && isRanged(player, target)) {
            List<BeastStrengthFamily> charging = QiWaveService.getChargeFamilies(
                    branch == StrengthPathBranch.BEAST_STRENGTH_PHANTOM, released,
                    StrengthRound.getAvailableFamilies(data), player.getRandom());
            shoot(player, target, damage, BeastChargeDamage.getDamages(data, charging, released,
                    StoreStrength.CHARGED_RELEASE_FACTOR, BeastChargeDamage.UNCAPPED), charging,
                    StoreStrength.getBurstRadius(maxPoints, widened));
        } else {
            swing(player, target, damage, maxPoints, widened);
        }
        BodyOverload.recoil(player, StoreStrength.getBacklashPoints(maxPoints, snapshot.capacity()),
                snapshot.capacity(), ModDamageTypes.IMPACT_BACKLASH);
    }

    private static boolean isRanged(@NotNull ServerPlayer player, @NotNull LivingEntity target) {
        return player.hasEffect(ModEffects.STRENGTH_QI_GU) && target.canBeHitByProjectile()
                && QiWaveService.isPastReach(QiWaveService.getBoxDistance(player, target),
                player.entityInteractionRange())
                && player.hasLineOfSight(target);
    }

    public static float getDamage(float damage, boolean locked) { return locked ? damage : damage * SECONDARY_RATIO; }

    private static List<BeastStrengthFamily> getShownFamilies(ServerPlayer player, List<BeastStrengthFamily> released) {
        List<BeastStrengthFamily> shown = new ArrayList<>(StrengthRound.getFamilies(player));
        for (BeastStrengthFamily family : released) {
            if (!shown.contains(family)) shown.add(family);
        }
        return shown;
    }

    //region Ranged [远程]
    private static void shoot(ServerPlayer player, LivingEntity target, float damage, float[] chargeDamages,
                              List<BeastStrengthFamily> charging, double radius) {
        QiBurst.Spec burst = new QiBurst.Spec(radius, ModDamageTypes.CHARGED_PUNCH, false);
        if (charging.isEmpty()) {
            QiWaveEntity.track(player, target, damage, burst);
            return;
        }
        for (int i = 0; i < charging.size(); i++) {
            BeastChargeEntity.shoot(player, target, chargeDamages[i], charging.get(i), burst, i, charging.size());
        }
    }
    //endregion

    //region Melee [近战]
    private static void swing(ServerPlayer player, @Nullable LivingEntity locked, float damage, int maxPoints,
                              boolean widened) {
        Vec3 feet = player.position();
        Vec3 axis = StoreStrength.getLevelAxis(locked != null ? locked.position().subtract(feet)
                : player.getLookAngle());
        if (axis == Vec3.ZERO) axis = StoreStrength.getLevelAxis(Vec3.directionFromRotation(0.0F, player.getYRot()));
        double range = StoreStrength.getRange(maxPoints, widened);
        double halfAngle = StoreStrength.getHalfAngle(widened);

        Vec3 fanAxis = axis;
        List<LivingEntity> targets = new ArrayList<>(player.level().getEntitiesOfClass(LivingEntity.class,
                player.getBoundingBox().inflate(range + 2.0D, StoreStrength.ABOVE + StoreStrength.BELOW,
                        range + 2.0D),
                target -> AreaImpact.canHit(player, target)
                        && StoreStrength.isInFan(feet, player.getBbHeight(), fanAxis, range, halfAngle,
                        target.getBoundingBox())
                        && player.hasLineOfSight(target)));
        targets.sort(Comparator.comparingDouble((LivingEntity target) -> target == locked ? -1.0D
                : target.distanceToSqr(feet)));

        DamageSource source = ModDamageTypes.source(player, player, ModDamageTypes.CHARGED_PUNCH);
        int landed = 0;
        for (LivingEntity target : targets) {
            if (landed >= MAX_TARGETS) break;
            if (!target.hurt(source, getDamage(damage, target == locked))) continue;

            Vec3 push = StoreStrength.getLevelAxis(target.position().subtract(feet));
            if (push == Vec3.ZERO) push = axis;
            target.knockback(KNOCKBACK, -push.x, -push.z);
            target.hurtMarked = true;
            landed++;
        }
        player.swing(InteractionHand.MAIN_HAND, true);
        if (AreaImpactShatter.canShatter(maxPoints)) {
            Vec3 eye = player.getEyePosition();
            AreaImpactShatter.shatter(player, eye, AreaImpactShatter.getFanCells(feet, player.getBbHeight(), axis,
                    range / 2.0D, halfAngle, eye), maxPoints, AreaImpactShatter.FAN_MAX_BLOCKS);
        }
        showSwing(player, axis, range);
    }

    private static void showSwing(ServerPlayer player, Vec3 axis, double range) {
        ServerLevel level = player.serverLevel();
        RingConeEmitter.areaImpactRing(level, player.position().add(0.0D, player.getBbHeight() * 0.5D, 0.0D), axis,
                range);
        level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.PLAYER_ATTACK_SWEEP,
                SoundSource.PLAYERS, 1.0F, 0.6F);
        level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.PLAYER_ATTACK_KNOCKBACK,
                SoundSource.PLAYERS, 1.0F, 0.8F);
    }
    //endregion
}
