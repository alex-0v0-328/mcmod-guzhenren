package net.alex.guzhenren.entity;

import java.util.function.Supplier;
import net.alex.guzhenren.entity.ai.HoverNearPlayerGoal;
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
 * A wild Gu that flies: it drifts toward a player it wants, and hovers there.
 *
 * <p>Extends {@link net.alex.guzhenren.entity.WildGuEntity}. A flying move control and flying path
 * navigation support the hover and random-flight goals. {@code seeks} is the one door: the base wants anyone,
 * a leaf narrows it; {@link #wants} is the same test used both for starting and for keeping a target,
 * so a player who turns spectator or leaves is let go.
 *
 * <p>⚠ {@code isNoGravity()} is a flat true on purpose. The flying move control only clears gravity
 * while it is actively moving the mob, and the hover goal stops the navigation. {@link #checkFallDamage}
 * likewise neutralizes the fall check: every descent is self-propelled (hover approach, landing goal,
 * escape cone), so the accrued fall distance must never become damage and must never kill the
 * one-health mote -- vanilla flyers such as bees and bats clear this check the same way.
 *
 * @author Alex
 * @version 1.0.0
 * @see net.alex.guzhenren.entity.WildGuEntity
 * @see net.alex.guzhenren.entity.ai.HoverNearPlayerGoal
 * @since 1.0.0
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

    //region who it flies toward -- the base wants anyone, a leaf narrows it
    public boolean seeks(Player player) { return true; }

    public @Nullable Player seekTarget() {
        return level().getNearestPlayer(getX(), getY(), getZ(), DETECT_RANGE, this::wanted);
    }

    public boolean wants(Player player) {
        return player.level() == level() && !player.isSpectator() && seeks(player);
    }

    private boolean wanted(Entity entity) {
        return entity instanceof Player player && wants(player);
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
