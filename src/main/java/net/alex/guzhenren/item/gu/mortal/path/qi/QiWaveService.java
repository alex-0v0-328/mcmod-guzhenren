package net.alex.guzhenren.item.gu.mortal.path.qi;

import java.util.ArrayList;
import java.util.List;
import net.alex.guzhenren.effect.timed.StrengthQiGuEffect;
import net.alex.guzhenren.entity.projectile.BeastChargeEntity;
import net.alex.guzhenren.entity.projectile.QiBurst;
import net.alex.guzhenren.entity.projectile.QiWaveEntity;
import net.alex.guzhenren.gameplay.path.strength.BeastChargeDamage;
import net.alex.guzhenren.gameplay.path.strength.BeastStrengthFamily;
import net.alex.guzhenren.gameplay.path.strength.PathStrengthData;
import net.alex.guzhenren.gameplay.path.strength.PathStrengthService;
import net.alex.guzhenren.gameplay.path.strength.StrengthPathBranch;
import net.alex.guzhenren.gameplay.path.strength.StrengthRound;
import net.alex.guzhenren.gameplay.path.strength.StrengthService;
import net.alex.guzhenren.item.gu.mortal.behavior.TimedEffects;
import net.alex.guzhenren.registry.effect.ModEffects;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;

/**
 * 力气蛊[Strength Qi Gu]（{@link StrengthQiGuEffect}）远程拳的服务端发射服务。
 * <ul>
 *   <li>处于力气蛊增益期间，玩家空手攻击原版近战触及距离外的 Epic Fight 锁定目标时发射远程拳弹射物（客户端仅上报目标实
 *       体 ID，服务端 {@link #fire} 全量复验，射程仅受原版 128 格视线限制）。</li>
 *   <li>{@link #isPastReach} 是双端共用的唯一射程分界：触及距离内留给原版近战，保证同一拳不会近战与远程双重命中；在攻击
 *       轮次内受 {@link StrengthRound#canRange} 间隔限制，轮次外挥拳即开启新一轮
 *       （{@link StrengthRound#startRange}）。</li>
 *   <li>兽力虚影流派按当前轮次已抽中的兽种分别发射一道 {@link BeastChargeEntity}，若本轮未抽中则从玩家已持有的永久兽力
 *       中随机抽取一种；不可显形的兽种（{@code BeastStrengthFamily#canManifest}）不发射冲击，将份额让给其他可显形兽
 *       种。</li>
 *   <li>每道兽力冲击按对应兽种的永久力量点[Strength Points]计算基础伤害（{@link BeastChargeDamage}），若本轮触发全面解
 *       放则乘 3，所有冲击的总伤害上限截断至当前肉身承受力；苦力蛊[Hardship Strength Gu]生效期间连续出拳每轮多抽一种兽
 *       力，直至全部可显形兽影同出。</li>
 *   <li>其他流派或未持有可显形永久兽力的玩家改为发射一枚 {@link QiWaveEntity}：基础伤害等同于当次空手拳伤（含力道加成与
 *       本轮解放加成，并补入本次新开轮次尚未计入属性的解放增量），不随原版攻击蓄力条缩放，改由远程发射间隔控制节奏；所
 *       有远程拳弹射物在落点均通过 {@link QiBurst} 爆开并各自独立判定一次范围冲击。</li>
 * </ul>
 */

public final class QiWaveService {

    private QiWaveService() {}

    public static boolean isPastReach(double distance, double reach) {
        return distance > reach;
    }

    public static boolean canPunch(@NotNull Player player) {
        return player.getMainHandItem().isEmpty() && player.isAlive() && !player.isSpectator();
    }

    public static double getBoxDistance(@NotNull Player player, @NotNull LivingEntity target) {
        return Math.sqrt(target.getBoundingBox().distanceToSqr(player.getEyePosition()));
    }

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

    public static @NotNull List<Projectile> fire(@NotNull ServerPlayer player, int targetId) {
        if (!canPunch(player) || !TimedEffects.hasBuff(player, ModEffects.STRENGTH_QI_GU)) return List.of();
        if (!(player.level().getEntity(targetId) instanceof LivingEntity target) || target == player) return List.of();
        if (!target.isAlive() || !target.canBeHitByProjectile()) return List.of();

        if (!isPastReach(getBoxDistance(player, target), player.entityInteractionRange())) return List.of();
        Vec3 toTarget = target.getBoundingBox().getCenter().subtract(player.getEyePosition());
        if (toTarget.dot(player.getLookAngle()) <= 0.0D) return List.of();
        if (!player.hasLineOfSight(target) || !StrengthRound.canRange(player)) return List.of();

        float gain = StrengthRound.startRange(player, player.getRandom().nextFloat());
        PathStrengthData data = PathStrengthService.get(player);
        List<BeastStrengthFamily> families = getChargeFamilies(
                PathStrengthService.getHeldBranch(player) == StrengthPathBranch.BEAST_STRENGTH_PHANTOM,
                StrengthRound.getFamilies(player), StrengthRound.getAvailableFamilies(data), player.getRandom());
        if (families.isEmpty()) {
            return List.of(QiWaveEntity.shoot(player, target,
                    (float) player.getAttributeValue(Attributes.ATTACK_DAMAGE) + gain));
        }

        float[] damages = BeastChargeDamage.getDamages(data, families, StrengthRound.getFullyReleased(player),
                StrengthService.FULL_RELEASE_FACTOR, StrengthService.getCapacity(player));
        List<Projectile> charges = new ArrayList<>();
        for (int i = 0; i < families.size(); i++) {
            charges.add(BeastChargeEntity.shoot(player, target, damages[i], families.get(i), QiBurst.Spec.QI_WAVE, i,
                    families.size()));
        }
        return charges;
    }
}
