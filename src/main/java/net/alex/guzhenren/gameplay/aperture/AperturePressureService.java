package net.alex.guzhenren.gameplay.aperture;

import net.alex.guzhenren.core.Ticks;
import net.alex.guzhenren.gameplay.body.BodyService;
import net.alex.guzhenren.gameplay.body.ExtremePhysique;
import net.alex.guzhenren.registry.attachment.ModAttachments;
import net.alex.guzhenren.registry.damage.ModDamageTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import org.jetbrains.annotations.NotNull;

/**
 * Aperture pressure [空窍压力]: a ten-extreme body's [十绝体] aperture fills with pressure and, once full,
 * detonates. Only an extreme body's aperture at {@code ApertureData.PRIMARY} carries pressure; every other
 * call is a no-op. Static service over the Aperture attachment.
 *
 * <p>{@link #tick} adds {@code PRESSURE_PER_MINUTE} once a minute up to {@code PRESSURE_COUNTDOWN_START},
 * then arms a {@code COUNTDOWN_TICKS} deadline; at the deadline the pressure is full ({@link #isFull}) and
 * the heartbeat's lethal check calls {@link #detonate}. {@link #detonate} settles entity damage and the
 * self-kill at tick zero and hands the crater to {@link AperturePressureExplosionTask}: the radius is
 * {@code EXPLOSION_RADIUS_PER_RANK} per rank, plus {@code GREAT_STRENGTH_RADIUS_BONUS} for Great Strength
 * True Martial [大力真武体], 112 at rank five.
 *
 * <p>⚠ Pressure writes go straight to the attachment, not through {@code ApertureService.store}: they
 * change neither rank nor stage, so health and Epic Fight need no refresh.
 *
 * @author Alex
 * @version 1.0.0
 * @see ApertureService
 * @see AperturePressureExplosionTask
 * @since 1.0.0
 */

public final class AperturePressureService {

    private AperturePressureService() {}

    private static final int PRESSURE_PER_MINUTE = 2;
    private static final int COUNTDOWN_TICKS = Ticks.HALF_MINUTE;
    private static final int EXPLOSION_RADIUS_PER_RANK = 16;
    private static final int GREAT_STRENGTH_RADIUS_BONUS = 16;
    private static final float DISASTER_DAMAGE = 10_000.0F;

    public static void set(@NotNull ServerPlayer player, int index, int value) {
        Aperture current = ApertureService.aperture(player, index);
        if (!BodyService.isExtreme(player) || index != ApertureData.PRIMARY) return;
        long deadline = value == Aperture.PRESSURE_COUNTDOWN_START ? current.pressureDeadlineTick() : 0L;
        if (value == Aperture.PRESSURE_COUNTDOWN_START && deadline == 0L) {
            deadline = player.level().getGameTime() + COUNTDOWN_TICKS;
        }
        if (current.pressure() == value && current.pressureDeadlineTick() == deadline) return;
        setState(player, index, value, deadline);
    }

    public static void relieve(@NotNull ServerPlayer player, int amount) {
        Aperture current = ApertureService.aperture(player, ApertureData.PRIMARY);
        if (!BodyService.isExtreme(player)) return;
        set(player, ApertureData.PRIMARY, Math.max(0, current.pressure() - amount));
    }

    public static void tick(@NotNull ServerPlayer player) {
        Aperture aperture = ApertureService.aperture(player, ApertureData.PRIMARY);
        if (!BodyService.isExtreme(player) || aperture.pressure() >= Aperture.MAX_PRESSURE) return;

        if (aperture.pressure() < Aperture.PRESSURE_COUNTDOWN_START) {
            if (player.tickCount % Ticks.MINUTE != 0) return;
            int next = Math.min(Aperture.PRESSURE_COUNTDOWN_START,
                    aperture.pressure() + PRESSURE_PER_MINUTE);
            long deadline = next == Aperture.PRESSURE_COUNTDOWN_START
                    ? player.level().getGameTime() + COUNTDOWN_TICKS : 0L;
            setState(player, ApertureData.PRIMARY, next, deadline);
            return;
        }

        long deadline = aperture.pressureDeadlineTick();
        if (deadline == 0L) {
            setState(player, ApertureData.PRIMARY, Aperture.PRESSURE_COUNTDOWN_START,
                    player.level().getGameTime() + COUNTDOWN_TICKS);
            return;
        }
        if (player.level().getGameTime() >= deadline) set(player, ApertureData.PRIMARY, Aperture.MAX_PRESSURE);
    }

    public static boolean isFull(@NotNull Player player) {
        Aperture aperture = ApertureService.aperture(player, ApertureData.PRIMARY);
        return BodyService.isExtreme(player) && aperture.pressure() >= Aperture.MAX_PRESSURE;
    }

    public static long getRemainingTicks(@NotNull Player player) {
        Aperture aperture = ApertureService.aperture(player, ApertureData.PRIMARY);
        if (!BodyService.isExtreme(player) || aperture.pressure() != Aperture.PRESSURE_COUNTDOWN_START
                || aperture.pressureDeadlineTick() <= 0L) return 0L;
        return Math.max(0L, aperture.pressureDeadlineTick() - player.level().getGameTime());
    }

    public static void detonate(@NotNull ServerPlayer player) {
        Aperture aperture = ApertureService.aperture(player);
        ExtremePhysique physique = BodyService.getExtremePhysique(player);
        int radius = getExplosionRadius(aperture.rank(), physique);
        double x = player.getX();
        double y = player.getY();
        double z = player.getZ();
        set(player, ApertureData.PRIMARY, 0);
        DamageSource source = ModDamageTypes.source(player, ModDamageTypes.APERTURE_PRESSURE_EXPLOSION);
        player.level().explode(null, source, null, x, y, z, 0.0F, false, Level.ExplosionInteraction.NONE);
        AperturePressureExplosionTask.start((ServerLevel) player.level(), x, y, z, radius, physique);

        DamageSource disaster = ModDamageTypes.source(player, ModDamageTypes.TEN_EXTREME_DISASTER);
        double radiusSquared = radius * (double) radius;
        AABB bounds = new AABB(x - radius, y - radius, z - radius, x + radius, y + radius, z + radius);
        for (Entity entity : player.level().getEntities(player, bounds, Entity::isAlive)) {
            if (entity.distanceToSqr(x, y, z) <= radiusSquared) entity.hurt(disaster, DISASTER_DAMAGE);
        }
        if (!player.isDeadOrDying()) player.hurt(source, Float.MAX_VALUE);
    }

    private static int getExplosionRadius(Rank rank, ExtremePhysique physique) {
        int rankStep = Math.clamp(rank.ordinal(), Rank.LOWEST.ordinal(), Rank.HIGHEST.ordinal()) + 1;
        int radius = EXPLOSION_RADIUS_PER_RANK * rankStep;
        return physique == ExtremePhysique.GREAT_STRENGTH_TRUE_MARTIAL ? radius + GREAT_STRENGTH_RADIUS_BONUS : radius;
    }

    private static void setState(ServerPlayer player, int index, int value, long deadline) {
        Aperture current = ApertureService.aperture(player, index);
        if (!BodyService.isExtreme(player) || index != ApertureData.PRIMARY
                || (current.pressure() == value && current.pressureDeadlineTick() == deadline)) return;
        player.setData(ModAttachments.APERTURE,
                ApertureService.get(player).with(index, current.withPressureAndDeadline(value, deadline)));
    }
}
