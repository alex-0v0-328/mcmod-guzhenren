package net.alex.guzhenren.gameplay.path.strength;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import net.alex.guzhenren.Guzhenren;
import net.alex.guzhenren.core.ModEvents;
import net.alex.guzhenren.registry.damage.ModDamageTypes;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import org.jetbrains.annotations.NotNull;

/**
 * 终点破坏打击[Crash Strike]与起点坑服务：跟踪横冲系冲撞的客户端位移，在冲出起点后补挖起点坑、停下时触发前方球形打击与
 * 终点崩裂。
 * <ul>
 *   <li>{@link #start} 在服务端发起冲撞时登记 {@code Run}：因位移由客户端驱动，服务端每 tick 测量上报位置的水平步长；当
 *       累计位移达到 {@link #MIN_TRAVEL} 且连续 {@link #STOP_TICKS} tick 步长小于 {@link #STOP_STEP}，或到达包含网络延
 *       迟余量的截止时限 {@link #getDeadline} 时判定停下（{@link #hasStopped}）。若累计位移超出预期路程加
 *       {@link #LOST_MARGIN}（发生传送）、死亡、登出或切换维度，则作废本次登记。</li>
 *   <li>终点打击球心位于停点身体中心沿冲撞方向前伸 {@link #AHEAD} 格处，半径取 {@link CrashSources#getStrikeRange}，视
 *       线遮挡从玩家身体中心起算（防止貼墙停下时球心嵌入墙内导致全被遮挡），按由近到远最多命中 {@link #MAX_TARGETS} 个
 *       目标。</li>
 *   <li>伤害 = 剥离主手武器修饰后的空手攻击力（{@link AttackDamageService#getBareFistDamage}）× {@link #getDamageRatio}
 *       （{@code guzhenren:crash_strike} 战斗伤害，受护甲减免，沿冲撞方向施加 {@link #KNOCKBACK} 击退）；不结算连招轮
 *       （不抽兽种、不出虚影），命中至少一个目标时结算一次 {@link BodyOverload#recoil} 肉体过载。</li>
 *   <li>终点打击后以停点为中心执行圆盘地形崩裂（{@link #getEndCells}，半径为打击半径一半，从脚底平层到头顶上一格，保留
 *       脚下所踩地面，至多破 {@link #MAX_BLOCKS} 块），并发布 {@link CrashStruckEvent} 触发冲击环粒子（挥空同样崩裂并发
 *       事件）。</li>
 *   <li>起点坑（{@link #getPitCells}，比终点圆盘多向下挖一层地面）：不在起冲第 0 tick 挖掘（否则 Epic Fight 前 2 tick
 *       原地过渡会让玩家掉进坑里被坑沿卡停），而是等玩家水平离开起点超过 {@code radius + PIT_CLEARANCE} 时再回头补挖；
 *       若未走出该距离即停下，挖坑时会自动排除玩家当前站立碰撞箱正下方的地面格（{@code isUnder}）。</li>
 *   <li>所有高度计算固定取站立姿态身高（{@link Pose#STANDING}），不受 Epic Fight 闪避动作压低碰撞箱的影响；终点打击受
 *       {@link #COOLDOWN_TICKS} 冷却限制（冷却期间仍可正常冲撞并挖起点坑，仅跳过终点打击与终点崩裂）。</li>
 *   <li>⚠ 若冲撞途中的网络延迟突增超出起冲登记时的往返延迟，兜底截止时限可能提前触发，使终点打击落在半途。</li>
 * </ul>
 */

@EventBusSubscriber(modid = Guzhenren.MOD_ID)
public final class CrashStrike {

    private CrashStrike() {}

    public static final double AHEAD = 1.0D;
    public static final double MIN_TRAVEL = 1.0D;
    public static final double STOP_STEP = 0.5D;
    public static final int LATE_TICKS = 3;
    public static final int STOP_TICKS = 2;
    public static final int LINK_TICKS = 2;
    public static final int MAX_LATENCY_TICKS = 20;
    public static final double LOST_MARGIN = 4.0D;
    public static final int MAX_TARGETS = 8;
    public static final double KNOCKBACK = 1.0D;
    public static final int MAX_BLOCKS = 64;
    public static final int PIT_FLOOR_LAYERS = 1;
    public static final double PIT_CLEARANCE = 1.2D;
    public static final int COOLDOWN_TICKS = 20;
    public static final float BASE_RATIO = 0.5F;
    public static final float TWO_BOARS_RATIO = 0.25F;
    public static final float ALL_OUT_RATIO = 0.25F;
    private static final float SOUND_VOLUME = 1.0F;
    private static final float SOUND_PITCH = 0.7F;

    private static final Map<UUID, Run> RUNS = new HashMap<>();
    private static final Map<UUID, Long> STRUCK = new HashMap<>();

    private static final class Run {

        final ResourceKey<Level> dimension;
        final Vec3 direction;
        final CrashSources sources;
        final Vec3 start;
        final int deadline;
        Vec3 last;
        double travelled;
        int slow;
        int ticks;
        boolean dug;

        Run(ResourceKey<Level> dimension, Vec3 direction, CrashSources sources, Vec3 start, int deadline) {
            this.dimension = dimension;
            this.direction = direction;
            this.sources = sources;
            this.start = start;
            this.last = start;
            this.deadline = deadline;
        }

        Outcome advance(Vec3 position) {
            double step = Math.hypot(position.x - last.x, position.z - last.z);
            last = position;
            travelled += step;
            slow = step < STOP_STEP ? slow + 1 : 0;
            ticks++;
            if (travelled > sources.getTravel() + LOST_MARGIN) return Outcome.LOST;
            return hasStopped(travelled, slow, ticks, deadline) ? Outcome.STOPPED : Outcome.MOVING;
        }
    }

    private enum Outcome { MOVING, STOPPED, LOST }

    //region Rules [规则]
    public static float getDamageRatio(boolean twoBoars, boolean allOut) {
        return BASE_RATIO + (twoBoars ? TWO_BOARS_RATIO : 0.0F) + (allOut ? ALL_OUT_RATIO : 0.0F);
    }

    public static boolean hasStopped(double travelled, int slowTicks, int ticks, int deadline) {
        return ticks >= deadline || travelled >= MIN_TRAVEL && slowTicks >= STOP_TICKS;
    }

    public static int getDeadline(double playSpeed, double travel, int latencyMillis) {
        int stretched = (int) Math.ceil(travel / CrashDash.MAX_STEP);
        int late = Math.min(MAX_LATENCY_TICKS, Math.max(0, (int) Math.ceil(latencyMillis / 50.0D)));
        return LINK_TICKS + CrashDash.getClipTicks(playSpeed) + stretched + LATE_TICKS + late;
    }

    public static boolean hasLeftPit(@NotNull Vec3 start, @NotNull Vec3 position, double radius) {
        return Math.hypot(position.x - start.x, position.z - start.z) > radius + PIT_CLEARANCE;
    }

    public static @NotNull List<BlockPos> getEndCells(@NotNull Vec3 feet, double height, double radius) {
        return AreaImpactShatter.getDiscCells(feet, height, radius, 0, feet.add(0.0D, height / 2.0D, 0.0D));
    }

    public static @NotNull List<BlockPos> getPitCells(@NotNull Vec3 start, double height, double radius,
                                                      @NotNull AABB standing) {
        int floor = Mth.floor(start.y + 0.5D) - PIT_FLOOR_LAYERS;
        List<BlockPos> cells = new ArrayList<>(AreaImpactShatter.getDiscCells(start, height, radius,
                PIT_FLOOR_LAYERS, start.add(0.0D, height / 2.0D, 0.0D)));
        cells.removeIf(pos -> pos.getY() <= floor && isUnder(pos, standing));
        return cells;
    }

    private static boolean isUnder(BlockPos pos, AABB box) {
        return pos.getX() < box.maxX && pos.getX() + 1 > box.minX && pos.getZ() < box.maxZ && pos.getZ() + 1 > box.minZ;
    }

    public static boolean isCooling(@NotNull ServerPlayer player) {
        Long struck = STRUCK.get(player.getUUID());
        return struck != null && player.level().getGameTime() - struck < COOLDOWN_TICKS;
    }
    //endregion

    //region Strike [打击]
    public static void start(@NotNull ServerPlayer player, @NotNull Vec3 direction, @NotNull CrashSources sources) {
        RUNS.put(player.getUUID(), new Run(player.level().dimension(), direction, sources, player.position(),
                getDeadline(sources.getPlaySpeed(), sources.getTravel(), player.connection.latency())));
    }

    public static @NotNull List<LivingEntity> strike(@NotNull ServerPlayer player, @NotNull Vec3 direction,
                                                     @NotNull CrashSources sources) {
        if (isCooling(player)) return List.of();
        STRUCK.put(player.getUUID(), player.level().getGameTime());

        double height = getStandingHeight(player);
        Vec3 body = player.position().add(0.0D, height / 2.0D, 0.0D);
        double range = sources.getStrikeRange();
        float damage = (float) (AttackDamageService.getBareFistDamage(player, sources.points())
                * sources.getDamageRatio());
        List<LivingEntity> landed = hit(player, body, body.add(direction.scale(AHEAD)), range, direction, damage);
        if (!landed.isEmpty()) BodyOverload.recoil(player);

        float hardness = sources.getMaxHardness();
        if (hardness >= 0.0F) {
            AreaImpactShatter.shatterWith(player, body, getEndCells(player.position(), height, range / 2.0D),
                    hardness, MAX_BLOCKS);
        }
        ServerLevel level = player.serverLevel();
        ModEvents.post(new CrashStruckEvent(player, body, direction, range));
        level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.PLAYER_ATTACK_KNOCKBACK,
                SoundSource.PLAYERS, SOUND_VOLUME, SOUND_PITCH);
        return landed;
    }

    public static int digPit(@NotNull ServerPlayer player, @NotNull Vec3 start, @NotNull CrashSources sources) {
        float hardness = sources.getMaxHardness();
        if (hardness < 0.0F) return 0;

        double height = getStandingHeight(player);
        return AreaImpactShatter.shatterWith(player, start.add(0.0D, height / 2.0D, 0.0D),
                getPitCells(start, height, sources.getStrikeRange() / 2.0D, player.getBoundingBox()), hardness,
                MAX_BLOCKS);
    }

    private static double getStandingHeight(ServerPlayer player) {
        return player.getDimensions(Pose.STANDING).height();
    }

    private static List<LivingEntity> hit(ServerPlayer player, Vec3 body, Vec3 center, double range, Vec3 direction,
                                          float damage) {
        List<LivingEntity> targets = player.level().getEntitiesOfClass(LivingEntity.class,
                new AABB(center, center).inflate(range),
                target -> AreaImpact.canHit(player, target)
                        && target.getBoundingBox().getCenter().distanceToSqr(center) <= range * range
                        && AreaImpact.isInSight(player, body, target));
        targets.sort(Comparator.comparingDouble(target -> target.getBoundingBox().getCenter().distanceToSqr(center)));

        DamageSource source = ModDamageTypes.source(player, player, ModDamageTypes.CRASH_STRIKE);
        List<LivingEntity> landed = new ArrayList<>();
        for (LivingEntity target : targets) {
            if (landed.size() >= MAX_TARGETS) break;
            if (!target.hurt(source, damage)) continue;

            target.knockback(KNOCKBACK, -direction.x, -direction.z);
            target.hurtMarked = true;
            landed.add(target);
        }
        return landed;
    }
    //endregion

    //region Events [事件]
    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        Run run = RUNS.get(player.getUUID());
        if (run == null) return;

        if (!player.isAlive() || !player.level().dimension().equals(run.dimension)) {
            RUNS.remove(player.getUUID());
            return;
        }
        Outcome outcome = run.advance(player.position());
        if (!run.dug && (outcome != Outcome.MOVING
                || hasLeftPit(run.start, player.position(), run.sources.getStrikeRange() / 2.0D))) {
            run.dug = true;
            digPit(player, run.start, run.sources);
        }
        if (outcome == Outcome.MOVING) return;
        RUNS.remove(player.getUUID());
        if (outcome == Outcome.STOPPED) strike(player, run.direction, run.sources);
    }

    @SubscribeEvent
    public static void onDeath(LivingDeathEvent event) { forget(event.getEntity()); }

    @SubscribeEvent
    public static void onLoggedOut(PlayerEvent.PlayerLoggedOutEvent event) {
        forget(event.getEntity());
        STRUCK.remove(event.getEntity().getUUID());
    }

    @SubscribeEvent
    public static void onChangedDimension(PlayerEvent.PlayerChangedDimensionEvent event) { forget(event.getEntity()); }

    @SubscribeEvent
    public static void onServerStopped(ServerStoppedEvent event) {
        RUNS.clear();
        STRUCK.clear();
    }

    private static void forget(LivingEntity entity) {
        if (entity instanceof Player) RUNS.remove(entity.getUUID());
    }
    //endregion
}
