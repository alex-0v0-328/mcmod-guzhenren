package net.alex.guzhenren.gameplay.flight;

import it.unimi.dsi.fastutil.ints.Int2LongMap;
import it.unimi.dsi.fastutil.ints.Int2LongOpenHashMap;
import it.unimi.dsi.fastutil.longs.LongIterator;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.alex.guzhenren.core.ModEvents;
import net.alex.guzhenren.gameplay.data.PlayerData;
import net.alex.guzhenren.gameplay.impact.ImpactCrater;
import net.alex.guzhenren.registry.entity.ModEntityTypeTags;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.OwnableEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;

/**
 * 二段飞行[Second-Stage Flight]：疾跑飞行之上一档，含两端共用规则与服务端部分。
 * <p>进出：
 * <ul>
 *   <li>沿视线巡航，最高为普通飞行 {@link #SPEED_MULTIPLIER} 倍（{@link SecondStageCruise}）。</li>
 *   <li>{@link #canEnter}：授权到二段（{@link FlightService#getGrant}）、在飞、在疾跑；客户端发双击前问
 *       （{@code net.alex.guzhenren.client.input.SecondStageInput}），服务端在 {@link #toggle} 再问；疾跑条件挡住悬停
 *       （原版仅前进时疾跑）。</li>
 *   <li>{@link #start} 打标记（{@code PlayerData#setSecondStage}，同步所有可见客户端）、油门设
 *       {@link SecondStageCruise#ENTRY_THROTTLE}、发 {@link SecondStageStartedEvent} 供上层音爆，每次进入都爆。</li>
 *   <li>{@link #tick}（{@link FlightEvents}，每服务端 tick）推油门（{@link #stepThrottle}）：疾跑加满，停跑（松前进或擦
 *       墙）耗尽；{@link #canStay} 断即退：无疾跑且油门空、停飞（落地、双击跳跃）、失授权；再双击主动退
 *       （{@link #toggle}），飞行保留。</li>
 *   <li>服务端从疾跑指令得知疾跑，晚一个包；客户端自推油门
 *       （{@code net.alex.guzhenren.client.event.SecondStageEvents}）。</li>
 * </ul>
 * <p>移动与撞击：
 * <ul>
 *   <li>有标记时 {@link #travel} 替换原版飞行移动（{@code net.alex.guzhenren.mixin.PlayerMixin}），由移动玩家的一端跑：
 *       客户端是本机物理，服务端是每 tick 先模拟、再放回客户端上报位置的那步，原版鞘翅撞墙伤害靠它；速度朝「油门 ×
 *       视线」靠拢（{@link SecondStageCruise#nextVelocity}），跳跃、潜行不升降（{@code LocalPlayerMixin}）。</li>
 *   <li>撞人：过 {@link SecondStageCruise#BARRIER_SPEED} 时服务端移动前撞飞扫过的生物（玩家攻击伤害随速度，沿飞行方向推
 *       开），同一生物每 {@link SecondStageCruise#RAM_COOLDOWN_TICKS} 一次，飞行者不减速；真受伤才推开，按击退抗性减，
 *       伤害被拒（无 PvP、无敌、受伤无敌时间）则不动、下 tick 可再撞。</li>
 *   <li>不撞野生蛊、任一方认定的盟友（同队、宠物）、飞行者拥有物、旁观者、盔甲架。</li>
 *   <li>破块：同在移动前，过音障且 {@code ModEvents#canGrief} 允许时，服务端用带掉落的 {@code Level#destroyBlock}
 *       （{@link #shatter}，粒子、声音原版）撞碎之后 {@link SecondStageCruise#BREAK_LOOKAHEAD_TICKS} tick 扫过的带碰撞
 *       脆弱方块（{@link SecondStageCruise#isFragile}），飞行穿过。</li>
 *   <li>任一轴碰撞（含地面、天花板）在服务端按三维损速付鞘翅撞墙伤害；损速达音障即撞毁：油门、速度清零，该端发
 *       {@link SecondStageCrashEvent}（服务端出环、震附近，客户端本机震屏），服务端退出二段并像坠击一样炸坑
 *       （{@link ImpactCrater#blast}，威力 {@link SecondStageCruise#getCrashPower}，身体中部沿飞行方向前
 *       {@link #CRASH_AHEAD} 格）。</li>
 *   <li>一 tick 扫过的全是脆弱方块不算撞（{@link #isFragileOnly}）：客户端先于服务端破块到达玻璃，禁破坏时只顶住；
 *       不掉血、不撞毁，油门照疾跑增减。</li>
 *   <li>两端按各自速度判撞毁，服务端晚一包，音障附近可能一端撞毁、一端只掉血，油门在
 *       {@link SecondStageCruise#COAST_TICKS} 内消掉差别；平飞擦地几乎不损速，不掉血；落地非墙，原版着地停飞，二段随之
 *       结束。</li>
 * </ul>
 * <p>限制：
 * <ul>
 *   <li>⚠ 速度不写进会存档的 {@code Abilities}。</li>
 *   <li>⚠ {@link #SPEED_MULTIPLIER} 须低于约 18：原版「移动过快」检查只容许每次移动超出服务端速度 10 格，进入时服务端未
 *       追上客户端，超了会被拉回；取 6 约 65 格/秒（约 4 区块新地形），为疾跑飞行 3 倍，近鞘翅俯冲。</li>
 *   <li>标记与油门不存档、死亡即丢，重登、重生从二段外开始；换维度同一玩家，首 tick 重判。</li>
 * </ul>
 *
 * @author Alex
 * @version 1.0.0
 * @see FlightService
 * @since 1.0.0
 */

public final class SecondStageService {

    private SecondStageService() {}

    public static final float SPEED_MULTIPLIER = 6.0F;
    private static final float WALL_BIG_SOUND_DAMAGE = 4.0F;
    private static final double CRASH_AHEAD = 0.8D;
    private static final float RAM_VOLUME = 1.0F;
    private static final Map<UUID, Int2LongMap> RAMMED = new HashMap<>();

    public static boolean isActive(@NotNull Player player) { return PlayerData.get(player).isInSecondStage(); }

    public static boolean canEnter(@NotNull Player player) {
        return FlightService.getGrant(player).canFlySecondStage() && player.getAbilities().flying
                && player.isSprinting();
    }

    private static boolean canStay(@NotNull Player player, double throttle) {
        return FlightService.getGrant(player).canFlySecondStage() && player.getAbilities().flying
                && (player.isSprinting() || throttle > 0.0D);
    }

    public static void toggle(@NotNull ServerPlayer player) {
        if (isActive(player)) {
            stop(player);
        } else {
            start(player);
        }
    }

    public static boolean start(@NotNull ServerPlayer player) {
        if (isActive(player) || !canEnter(player)) return false;
        PlayerData data = PlayerData.get(player);
        data.setSecondStage(true);
        data.setSecondStageThrottle(SecondStageCruise.ENTRY_THROTTLE);
        ModEvents.post(new SecondStageStartedEvent(player));
        return true;
    }

    public static void stop(@NotNull ServerPlayer player) {
        PlayerData data = PlayerData.get(player);
        data.setSecondStage(false);
        data.setSecondStageThrottle(0.0D);
        RAMMED.remove(player.getUUID());
    }

    public static void forget(@NotNull Player player) { RAMMED.remove(player.getUUID()); }

    public static void tick(@NotNull ServerPlayer player) {
        if (isActive(player) && !canStay(player, stepThrottle(player))) stop(player);
    }

    public static double stepThrottle(@NotNull Player player) {
        PlayerData data = PlayerData.get(player);
        double throttle = SecondStageCruise.nextThrottle(data.getSecondStageThrottle(), player.isSprinting());
        data.setSecondStageThrottle(throttle);
        return throttle;
    }

    public static boolean travel(@NotNull Player player) {
        if (!player.getAbilities().flying || player.isPassenger() || !player.isControlledByLocalInstance()
                || !isActive(player)) {
            return false;
        }
        Vec3 velocity = SecondStageCruise.nextVelocity(player.getDeltaMovement(), player.getLookAngle(),
                PlayerData.get(player).getSecondStageThrottle(),
                SecondStageCruise.getTopSpeed(player.getAbilities().getFlyingSpeed()));
        if (player instanceof ServerPlayer server) {
            ram(server, velocity);
            shatter(server, velocity);
        }
        if (player.isFallFlying()) player.stopFallFlying();
        player.setDeltaMovement(velocity);
        AABB swept = player.getBoundingBox().expandTowards(velocity);
        player.move(MoverType.SELF, velocity);
        if ((player.horizontalCollision || player.verticalCollision) && !isFragileOnly(player.level(), swept)) {
            hitWall(player, velocity);
        }
        player.resetFallDistance();
        player.calculateEntityAnimation(false);
        return true;
    }

    private static void shatter(ServerPlayer player, Vec3 velocity) {
        if (velocity.length() < SecondStageCruise.BARRIER_SPEED || !ModEvents.canGrief(player.level(), player)) return;
        AABB swept = player.getBoundingBox().expandTowards(velocity.scale(SecondStageCruise.BREAK_LOOKAHEAD_TICKS));
        for (BlockPos pos : blocksIn(swept)) {
            if (isFragile(player.level(), pos, player.level().getBlockState(pos))) {
                player.level().destroyBlock(pos, true, player);
            }
        }
    }

    private static boolean isFragileOnly(Level level, AABB swept) {
        boolean found = false;
        for (BlockPos pos : blocksIn(swept)) {
            BlockState state = level.getBlockState(pos);
            if (state.isAir() || state.getCollisionShape(level, pos).isEmpty()) continue;
            if (!isFragile(level, pos, state)) return false;
            found = true;
        }
        return found;
    }

    private static Iterable<BlockPos> blocksIn(AABB box) {
        return BlockPos.betweenClosed(Mth.floor(box.minX), Mth.floor(box.minY), Mth.floor(box.minZ),
                Mth.ceil(box.maxX) - 1, Mth.ceil(box.maxY) - 1, Mth.ceil(box.maxZ) - 1);
    }

    @SuppressWarnings("deprecation")
    private static boolean isFragile(Level level, BlockPos pos, BlockState state) {
        return !state.isAir() && SecondStageCruise.isFragile(state.getBlock().getExplosionResistance())
                && !state.getCollisionShape(level, pos).isEmpty();
    }

    private static void hitWall(Player player, Vec3 velocity) {
        double lost = velocity.length() - player.getDeltaMovement().length();
        if (!player.level().isClientSide()) {
            float damage = SecondStageCruise.getWallDamage(lost);
            if (damage > 0.0F) {
                LivingEntity.Fallsounds sounds = player.getFallSounds();
                player.playSound((int) damage > WALL_BIG_SOUND_DAMAGE ? sounds.big() : sounds.small(), 1.0F, 1.0F);
                player.hurt(player.damageSources().flyIntoWall(), damage);
            }
        }
        if (!SecondStageCruise.isCrash(lost)) return;

        PlayerData.get(player).setSecondStageThrottle(0.0D);
        player.setDeltaMovement(Vec3.ZERO);
        Vec3 heading = velocity.normalize();
        ModEvents.post(new SecondStageCrashEvent(player, heading));
        if (!(player instanceof ServerPlayer server)) return;
        stop(server);
        Vec3 impact = player.position().add(0.0D, player.getBbHeight() / 2.0D, 0.0D).add(heading.scale(CRASH_AHEAD));
        ImpactCrater.blast(server, impact, SecondStageCruise.getCrashPower(lost));
    }

    private static void ram(ServerPlayer player, Vec3 velocity) {
        double speed = velocity.length();
        if (speed < SecondStageCruise.BARRIER_SPEED) return;
        long now = player.level().getGameTime();
        Int2LongMap rammed = RAMMED.computeIfAbsent(player.getUUID(), id -> new Int2LongOpenHashMap());
        for (LongIterator at = rammed.values().iterator(); at.hasNext(); ) {
            if (now - at.nextLong() >= SecondStageCruise.RAM_COOLDOWN_TICKS) at.remove();
        }

        AABB swept = player.getBoundingBox().expandTowards(velocity);
        for (LivingEntity target : player.level().getEntitiesOfClass(LivingEntity.class, swept,
                target -> canRam(player, target))) {
            if (rammed.containsKey(target.getId())) continue;
            rammed.put(target.getId(), now);
            if (!target.hurt(player.damageSources().playerAttack(player), SecondStageCruise.getRamDamage(speed))) {
                rammed.remove(target.getId());
                continue;
            }
            double resistance = Math.clamp(target.getAttributeValue(Attributes.KNOCKBACK_RESISTANCE), 0.0D, 1.0D);
            target.setDeltaMovement(SecondStageCruise.getRamPush(velocity).scale(1.0D - resistance));
            target.hasImpulse = true;
            target.hurtMarked = true;
            player.level().playSound(null, target.getX(), target.getY(), target.getZ(),
                    SoundEvents.PLAYER_ATTACK_KNOCKBACK, SoundSource.PLAYERS, RAM_VOLUME, 1.0F);
        }
    }

    private static boolean canRam(ServerPlayer player, LivingEntity target) {
        return target != player && target.isAlive() && !target.isSpectator() && target.isPickable()
                && !player.isAlliedTo(target) && !target.isAlliedTo(player)
                && !(target instanceof OwnableEntity owned && player.equals(owned.getOwner()))
                && !target.getType().is(ModEntityTypeTags.WILD_GU) && !(target instanceof ArmorStand);
    }
}
