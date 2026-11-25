package net.alex.guzhenren.entity.gu;

import java.util.function.Predicate;
import java.util.function.Supplier;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.control.FlyingMoveControl;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomFlyingGoal;
import net.minecraft.world.entity.ai.navigation.FlyingPathNavigation;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * 飞行野生蛊基类：继承 {@link WildGuEntity}，发现合意玩家时主动飘近并在其身旁悬停（{@link HoverNearPlayerGoal}）。
 * <ul>
 *   <li>{@link #isSeeking} 是筛选目标玩家的唯一子类钩子：本基类默认接受所有玩家，子类可收窄条件；{@link #isWanting} 同
 *       时用于初始锁定与持续保持，玩家切入旁观模式或离开当前维度时自动放弃。</li>
 *   <li>⚠ {@link #isNoGravity()} 恒返回 {@code true}：{@link FlyingMoveControl} 仅在寻路移动期间关闭重力，而悬停目标会
 *       停止寻路导航，若不常驻无重力会导致悬停时下坠。</li>
 *   <li>⚠ {@link #checkFallDamage} 必须置空：所有下降位移均由 AI 主动驱动（悬停、降落、逃跑锥），不能因累计下落距离摔
 *       死只有 1 点生命的野生蛊（与原版蜜蜂、蝙蝠一致）。</li>
 * </ul>
 */

public class FlyingGuEntity extends WildGuEntity {

    public static final double DETECT_RANGE = 12.0;
    public static final double HOVER_RANGE = 2.0;
    private static final double FOLLOW_RANGE = 16.0;
    private static final double MAX_HEALTH = 1.0;
    private static final double FLYING_SPEED = 0.1;
    private static final double MOVEMENT_SPEED = 0.1;
    private static final double WANDER_SPEED = 1.0;
    private static final int TURN_RATE = 20;
    private final Predicate<Entity> wanted = this::isWanted;

    public FlyingGuEntity(EntityType<? extends FlyingGuEntity> type, Level level,
                          Supplier<Item> caughtGu) {
        super(type, level, caughtGu);
        this.moveControl = new FlyingMoveControl(this, TURN_RATE, true);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Mob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, MAX_HEALTH)
                .add(Attributes.FLYING_SPEED, FLYING_SPEED)
                .add(Attributes.MOVEMENT_SPEED, MOVEMENT_SPEED)
                .add(Attributes.FOLLOW_RANGE, FOLLOW_RANGE);
    }

    @Override
    protected void registerGoals() {
        goalSelector.addGoal(0, new HoverNearPlayerGoal(this));
        goalSelector.addGoal(1, new WaterAvoidingRandomFlyingGoal(this, WANDER_SPEED));
    }

    //region targeting [飞向目标]
    public boolean isSeeking(Player player) { return true; }

    public @Nullable Player seekTarget() {
        return level().getNearestPlayer(getX(), getY(), getZ(), DETECT_RANGE, wanted);
    }

    public boolean isWanting(Player player) {
        return player.level() == level() && !player.isSpectator() && isSeeking(player);
    }

    private boolean isWanted(Entity entity) {
        return entity instanceof Player player && isWanting(player);
    }
    //endregion

    @Override
    protected @NotNull PathNavigation createNavigation(@NotNull Level level) {
        FlyingPathNavigation navigation = new FlyingPathNavigation(this, level);
        navigation.setCanOpenDoors(false);
        navigation.setCanFloat(true);
        return navigation;
    }

    @Override
    public boolean isNoGravity() { return true; }

    @Override
    protected void checkFallDamage(double y, boolean onGround, @NotNull BlockState state, @NotNull BlockPos pos) {
    }
}
