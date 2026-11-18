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
 * 二段飞行[Second-Stage Flight]服务：管理疾跑飞行之上的高速视线巡航、冲撞生物、撞碎脆弱方块与撞毁炸坑。
 * <ul>
 *   <li>{@link #canEnter} 要求玩家拥有二段飞行授权（{@link FlightService#getGrant}）、正在飞行且正在疾跑（客户端双击前
 *       与服务端 {@link #toggle} 均校验此条件，疾跑门槛自然排除悬停状态）。</li>
 *   <li>{@link #start} 设置二段飞行同步标记、将油门初始化为 {@link SecondStageCruise#ENTRY_THROTTLE}，并每次发布
 *       {@link SecondStageStartedEvent} 触发音爆特效。</li>
 *   <li>{@link #tick} 每个服务端 tick 推进油门（{@link #stepThrottle}，客户端在本地同步自推油门）：当停止疾跑且油门归
 *       零、落地停飞或失去授权时（{@code !canStay}）自动退出二段飞行；再次双击疾跑可主动调 {@link #toggle} 退出二段并保
 *       持普通飞行。</li>
 *   <li>二段飞行激活期间，{@link #travel} 接管原版飞行移动（{@code PlayerMixin}，由当前控制移动的一端执行，并屏蔽跳跃与
 *       潜行升降）：在服务端移动前，若速度达到音障（{@link SecondStageCruise#BARRIER_SPEED}），先执行 {@code ram} 撞飞
 *       路径上的非盟友/非野生蛊活体生物（按同一目标 {@link SecondStageCruise#RAM_COOLDOWN_TICKS} 冷却，仅在实际造成玩家
 *       攻击伤害时按击退抗性推开，飞行者自身不减速），并在 {@code ModEvents.canGrief} 允许时执行 {@code shatter} 提前撞
 *       碎前方 {@link SecondStageCruise#BREAK_LOOKAHEAD_TICKS} tick 内的脆弱方块。</li>
 *   <li>移动后若发生非纯脆弱方块碰撞（{@code !isFragileOnly}），服务端按三维速度损失结算原版鞘翅撞墙伤害；当速度损失达
 *       到音障阈值时判定为撞毁：清零油门与速度、在当前端发布 {@link SecondStageCrashEvent}，服务端退出二段飞行并在身体
 *       前方 {@code CRASH_AHEAD} 格处调 {@link ImpactCrater#blast} 炸坑。</li>
 *   <li>⚠ 服务端的模拟移动步比客户端早一 tick 接触墙面，因此客户端最后一刻转弯避开时服务端仍可能判定擦墙受伤（与原版鞘
 *       翅一致）。</li>
 *   <li>⚠ 二段速度绝不写入会落盘的 {@code Abilities}；{@link #SPEED_MULTIPLIER}（{@code 6.0}，约 65 格/秒）必须低于约
 *       {@code 18}，否则进入二段瞬间客户端单步位移超出服务端预期 10 格上限，会被原版「移动过快」检查强制拉回。二段标记
 *       与油门均不落盘，重登或死亡后自动归零。</li>
 * </ul>
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
