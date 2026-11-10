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
 * The Strength Qi Gu's [力气蛊] ranged punch: while its buff runs, an empty-hand punch at Epic Fight's lock-on target
 * past the vanilla reach throws one projectile at it, at any distance. The client sends the locked target's id;
 * {@link #fire} checks it all again, bounded only by line of sight (vanilla's own 128 blocks).
 *
 * <p>{@link #isPastReach} is the one range rule, read by both ends: a target inside the reach is left to the plain
 * melee hit, so one click never lands twice. The punch has its own spacing inside the strength rounds [攻击轮次]
 * ({@link StrengthRound#canRange}): {@code RANGE_INTERVAL_TICKS} apart, and one thrown outside a round opens one. A
 * Qi Wave hits as hard as the fist: the player's attack damage, where the strength path's bonus and the round's
 * release live, plus the release the round it opened has not yet put there. No attack charge scales it; the spacing
 * paces it.
 *
 * <p>A holder of the beast strength phantom path [兽力虚影流] throws one {@link BeastChargeEntity} for every family the
 * round drew, or one of a held family at random when it drew none ({@link #getChargeFamilies}). Each hits with the
 * strength its own family adds, not the punch's ({@link BeastChargeDamage}): its permanent points, three times over
 * when the round fully releases the family, the charges' sum cut to the current capacity; under Hardship Strength
 * [苦力] the streak draws one family more a round until every phantom strikes. Only a family that can manifest
 * ({@code BeastStrengthFamily.canManifest}) charges; one that cannot leaves its share to the others. Everyone else,
 * and a beast holder without a permanent beast that manifests, throws one {@link QiWaveEntity}. Each one bursts
 * ({@link QiBurst}) around where it lands, and each burst rolls the area impact [范围冲击] once on its own.
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
