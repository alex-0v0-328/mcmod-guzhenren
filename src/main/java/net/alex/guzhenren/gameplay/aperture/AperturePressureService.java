package net.alex.guzhenren.gameplay.aperture;

import net.alex.guzhenren.core.ModEvents;
import net.alex.guzhenren.core.Ticks;
import net.alex.guzhenren.gameplay.aperture.pressure.AperturePressureExplosionTask;
import net.alex.guzhenren.gameplay.body.BodyService;
import net.alex.guzhenren.gameplay.body.ExtremePhysique;
import net.alex.guzhenren.gameplay.data.PlayerData;
import net.alex.guzhenren.registry.damage.ModDamageTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import org.jetbrains.annotations.NotNull;

/**
 * 空窍压力静态服务：推进十绝体[Ten Extreme Physiques]主空窍的压力累积、倒计时与满压引爆。
 * <ul>
 *   <li>仅身怀十绝体且下标为 {@link ApertureData#PRIMARY} 的空窍会产生压力，其余情况调用均为空操作。</li>
 *   <li>{@link #tick} 每分钟（按 {@code tickCount}）增加 {@code PRESSURE_PER_MINUTE}，到达
 *       {@link Aperture#PRESSURE_COUNTDOWN_START} 时写入 {@code gameTime + COUNTDOWN_TICKS} 截止时刻；到期后置为满压
 *       （{@link #isFull}），由心跳致死检查调 {@link #detonate}。分钟步进与倒计时均按世界真实 tick 计，不受
 *       {@code TimeFlow} 自身时间倍率影响。</li>
 *   <li>{@link #detonate} 在当前 tick 立即结算范围内实体灾难伤害与玩家自身致死，地形挖坑则在通过
 *       {@code ModEvents.canGrief} 检查后交给 {@link AperturePressureExplosionTask#start} 分 tick 推进（若禁止破坏地
 *       形，伤害与自爆致死照常生效）。</li>
 *   <li>⚠ 压力变更通过 {@code setState} 直接写入 {@link PlayerData}，绕过 {@link ApertureService} 的 {@code store}，避
 *       免每次涨压都触发最大生命值刷新。</li>
 *   <li>⚠ 压力字段位于 {@link Aperture} 且温养、冲刷窍壁均需泄压，故本服务留在 {@code gameplay.aperture} 包内，仅单向
 *       调用 {@code pressure} 子包的 {@link AperturePressureExplosionTask#start}，避免父子包循环依赖。</li>
 * </ul>
 */

public final class AperturePressureService {

    private AperturePressureService() {}

    private static final int PRESSURE_PER_MINUTE = 2;
    private static final int COUNTDOWN_TICKS = Ticks.HALF_MINUTE;
    private static final int EXPLOSION_RADIUS_PER_RANK = 16;
    private static final int GREAT_STRENGTH_RADIUS_BONUS = 16;
    private static final float DISASTER_DAMAGE = 10_000.0F;

    public static void set(@NotNull ServerPlayer player, int index, int value) {
        if (!BodyService.isExtreme(player) || index != ApertureData.PRIMARY) return;
        long deadline = 0L;
        if (value == Aperture.PRESSURE_COUNTDOWN_START) {
            deadline = ApertureService.getAperture(player, index).pressureDeadlineTick();
            if (deadline == 0L) deadline = player.level().getGameTime() + COUNTDOWN_TICKS;
        }
        setState(player, value, deadline);
    }

    public static void relieve(@NotNull ServerPlayer player, int amount) {
        int pressure = ApertureService.getAperture(player, ApertureData.PRIMARY).pressure();
        set(player, ApertureData.PRIMARY, Math.max(0, pressure - amount));
    }

    public static void tick(@NotNull ServerPlayer player) {
        Aperture aperture = ApertureService.getAperture(player, ApertureData.PRIMARY);
        if (!BodyService.isExtreme(player) || aperture.pressure() >= Aperture.MAX_PRESSURE) return;

        if (aperture.pressure() < Aperture.PRESSURE_COUNTDOWN_START) {
            if (player.tickCount % Ticks.MINUTE != 0) return;
            int next = Math.min(Aperture.PRESSURE_COUNTDOWN_START,
                    aperture.pressure() + PRESSURE_PER_MINUTE);
            long deadline = next == Aperture.PRESSURE_COUNTDOWN_START
                    ? player.level().getGameTime() + COUNTDOWN_TICKS : 0L;
            setState(player, next, deadline);
            return;
        }

        long deadline = aperture.pressureDeadlineTick();
        if (deadline == 0L) {
            setState(player, Aperture.PRESSURE_COUNTDOWN_START, player.level().getGameTime() + COUNTDOWN_TICKS);
            return;
        }
        if (player.level().getGameTime() >= deadline) set(player, ApertureData.PRIMARY, Aperture.MAX_PRESSURE);
    }

    public static boolean isFull(@NotNull Player player) {
        Aperture aperture = ApertureService.getAperture(player, ApertureData.PRIMARY);
        return BodyService.isExtreme(player) && aperture.pressure() >= Aperture.MAX_PRESSURE;
    }

    public static long getRemainingTicks(@NotNull Player player) {
        Aperture aperture = ApertureService.getAperture(player, ApertureData.PRIMARY);
        if (!BodyService.isExtreme(player) || aperture.pressure() != Aperture.PRESSURE_COUNTDOWN_START
                || aperture.pressureDeadlineTick() <= 0L) return 0L;
        return Math.max(0L, aperture.pressureDeadlineTick() - player.level().getGameTime());
    }

    public static void detonate(@NotNull ServerPlayer player) {
        Aperture aperture = ApertureService.getAperture(player);
        ExtremePhysique physique = BodyService.getExtremePhysique(player);
        int radius = getExplosionRadius(aperture.rank(), physique);
        double x = player.getX();
        double y = player.getY();
        double z = player.getZ();
        set(player, ApertureData.PRIMARY, 0);
        DamageSource source = ModDamageTypes.source(player, ModDamageTypes.APERTURE_PRESSURE_EXPLOSION);
        player.level().explode(null, source, null, x, y, z, 0.0F, false, Level.ExplosionInteraction.NONE);
        if (ModEvents.canGrief(player.level(), player)) {
            AperturePressureExplosionTask.start((ServerLevel) player.level(), x, y, z, radius, physique);
        }

        DamageSource disaster = ModDamageTypes.source(player, ModDamageTypes.TEN_EXTREME_DISASTER);
        double radiusSquared = radius * (double) radius;
        AABB bounds = new AABB(x - radius, y - radius, z - radius, x + radius, y + radius, z + radius);
        for (Entity entity : player.level().getEntities(player, bounds, Entity::isAlive)) {
            if (entity.distanceToSqr(x, y, z) <= radiusSquared) entity.hurt(disaster, DISASTER_DAMAGE);
        }
        if (!player.isDeadOrDying()) player.hurt(source, Float.MAX_VALUE);
    }

    private static int getExplosionRadius(Rank rank, ExtremePhysique physique) {
        int rankStep = Mth.clamp(rank.ordinal(), Rank.LOWEST.ordinal(), Rank.HIGHEST.ordinal()) + 1;
        int radius = EXPLOSION_RADIUS_PER_RANK * rankStep;
        return physique == ExtremePhysique.GREAT_STRENGTH_TRUE_MARTIAL ? radius + GREAT_STRENGTH_RADIUS_BONUS : radius;
    }

    private static void setState(ServerPlayer player, int value, long deadline) {
        Aperture current = ApertureService.getAperture(player, ApertureData.PRIMARY);
        if (current.pressure() == value && current.pressureDeadlineTick() == deadline) return;
        PlayerData.get(player).setAperture(ApertureService.get(player)
                .with(ApertureData.PRIMARY, current.withPressureAndDeadline(value, deadline)));
    }
}
