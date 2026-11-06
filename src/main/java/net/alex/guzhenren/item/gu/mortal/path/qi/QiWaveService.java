package net.alex.guzhenren.item.gu.mortal.path.qi;

import java.util.List;
import net.alex.guzhenren.effect.timed.StrengthQiGuEffect;
import net.alex.guzhenren.entity.projectile.BeastChargeEntity;
import net.alex.guzhenren.entity.projectile.QiBurst;
import net.alex.guzhenren.entity.projectile.QiWaveEntity;
import net.alex.guzhenren.gameplay.path.strength.BeastStrengthFamily;
import net.alex.guzhenren.gameplay.path.strength.PathStrengthService;
import net.alex.guzhenren.gameplay.path.strength.StrengthPathBranch;
import net.alex.guzhenren.gameplay.path.strength.StrengthRound;
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
import org.jetbrains.annotations.Nullable;

/**
 * The Strength Qi Gu's [力气蛊] ranged punch: while its buff runs, an empty-hand punch at Epic Fight's lock-on target
 * past the vanilla reach throws one projectile at it, at any distance. The client sends the locked target's id;
 * {@link #fire} checks it all again, bounded only by line of sight (vanilla's own 128 blocks).
 *
 * <p>{@link #isPastReach} is the one range rule, read by both ends: a target inside the reach is left to the plain
 * melee hit, so one click never lands twice. The punch keeps to the strength rounds [攻击轮次]
 * ({@link StrengthRound#canRange}): one a round at most, and one thrown outside a round opens one. It hits as hard as
 * the fist: the player's attack damage, where the strength path's bonus and the round's release live, plus the release
 * the round it opened has not yet put there. No attack charge scales it; the rounds pace it.
 *
 * <p>A holder of the beast strength phantom path [兽力虚影流] throws a {@link BeastChargeEntity} of the round's first
 * drawn family, or of a held family at random ({@link #getChargeFamily}); everyone else, and a beast holder without a
 * permanent beast, throws a {@link QiWaveEntity}. Either one bursts ({@link QiBurst}) around where it lands.
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

    public static @Nullable BeastStrengthFamily getChargeFamily(boolean beastPath,
                                                                @NotNull List<BeastStrengthFamily> drawn,
                                                                @NotNull List<BeastStrengthFamily> held,
                                                                @NotNull RandomSource random) {
        if (!beastPath || held.isEmpty()) return null;
        if (!drawn.isEmpty()) return drawn.getFirst();
        return held.get(random.nextInt(held.size()));
    }

    public static @Nullable Projectile fire(@NotNull ServerPlayer player, int targetId) {
        if (!canPunch(player) || !TimedEffects.hasBuff(player, ModEffects.STRENGTH_QI_GU)) return null;
        if (!(player.level().getEntity(targetId) instanceof LivingEntity target) || target == player) return null;
        if (!target.isAlive() || !target.canBeHitByProjectile()) return null;

        if (!isPastReach(getBoxDistance(player, target), player.entityInteractionRange())) return null;
        Vec3 toTarget = target.getBoundingBox().getCenter().subtract(player.getEyePosition());
        if (toTarget.dot(player.getLookAngle()) <= 0.0D) return null;
        if (!player.hasLineOfSight(target) || !StrengthRound.canRange(player)) return null;

        float gain = StrengthRound.startRange(player, player.getRandom().nextFloat());
        float damage = (float) player.getAttributeValue(Attributes.ATTACK_DAMAGE) + gain;
        BeastStrengthFamily family = getChargeFamily(
                PathStrengthService.holdsBranch(player, StrengthPathBranch.BEAST_STRENGTH_PHANTOM),
                StrengthRound.getFamilies(player),
                StrengthRound.getAvailableFamilies(PathStrengthService.get(player)), player.getRandom());
        return family != null ? BeastChargeEntity.shoot(player, target, damage, family)
                : QiWaveEntity.shoot(player, target, damage);
    }
}
