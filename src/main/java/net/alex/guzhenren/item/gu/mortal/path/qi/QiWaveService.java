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
 * 力气蛊[Strength Qi Gu]的远程拳。
 * <ul>
 *   <li>增益期间空手打原版触及距离外的 Epic Fight 锁定目标，抛投射物，不限距离；客户端只发 id，{@link #fire}
 *       全部重验，只受视线限（原版 128 格）。</li>
 *   <li>{@link #isPastReach} 是两端共读的唯一射程规则：触及内留给近战，一击不中两次；轮次内间隔
 *       {@code RANGE_INTERVAL_TICKS}（{@link StrengthRound#canRange}），轮次外抛出即开一轮。</li>
 *   <li>兽力虚影流按本轮抽中兽种各抛一个 {@link BeastChargeEntity}，无则随机一个持有兽种；不能出形的不冲击，
 *       份额让给其他（{@code BeastStrengthFamily.canManifest}）。</li>
 *   <li>每个按本兽种永久力量点[Strength Points]命中（{@link BeastChargeDamage}），本轮全面解放时 ×3；总和截到
 *       当前承受力；苦力期间连击每轮多抽一种，直到虚影全出。</li>
 *   <li>其余人及无可出形永久兽力[Beast Strength]者抛 {@link QiWaveEntity}：伤害同拳（含力道加成与本轮解放），另加所开
 *       那轮未计入的解放；不按攻击蓄力缩放，由间隔控节奏；各投射物落点爆开（{@link QiBurst}），各掷一次范围冲击。</li>
 * </ul>
 *
 * @author Alex
 * @version 1.0.0
 * @see StrengthQiGuEffect
 * @since 1.0.0
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
