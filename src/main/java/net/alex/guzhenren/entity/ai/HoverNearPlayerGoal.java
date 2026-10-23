package net.alex.guzhenren.entity.ai;

import java.util.EnumSet;
import net.alex.guzhenren.entity.FlyingGuEntity;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

/**
 * The goal that makes a flying Gu hover beside the player it wants.
 *
 * <p>☠ Drives the movement vector directly and never paths -- a mote flies straight, so the navigation's
 * path cache and stuck detector would be obstacles rather than help. The target is re-read every tick via
 * {@link net.alex.guzhenren.entity.FlyingGuEntity#seekTarget}; a sine bob on {@code tickCount} gives
 * the hover its drift.
 *
 * @author Alex
 * @version 1.0.0
 * @see net.alex.guzhenren.entity.FlyingGuEntity
 * @since 1.0.0
 */

public class HoverNearPlayerGoal extends Goal {

    private static final double APPROACH_SPEED = 0.25;
    private static final double EASING = 0.25;
    private static final double BOB_AMPLITUDE = 0.15;
    private static final double BOB_FREQUENCY = 0.15;
    private static final double HOVER_DRAG = 0.8;
    private final FlyingGuEntity gu;
    private @Nullable Player target;

    public HoverNearPlayerGoal(FlyingGuEntity gu) {
        this.gu = gu;
        setFlags(EnumSet.of(Goal.Flag.MOVE, Goal.Flag.LOOK));
    }

    @Override
    public boolean canUse() {
        target = gu.seekTarget();
        return target != null;
    }

    @Override
    public boolean canContinueToUse() {
        return target != null && target.isAlive() && gu.wants(target)
                && gu.distanceToSqr(target) <= detectRangeSqr();
    }

    @Override
    public void start() { gu.getNavigation().stop(); }

    @Override
    public void stop() { target = null; }

    @Override
    public boolean requiresUpdateEveryTick() { return true; }

    @Override
    public void tick() {
        if (target == null) return;
        gu.getLookControl().setLookAt(target);

        Vec3 toEye = target.getEyePosition().subtract(gu.position());
        if (toEye.lengthSqr() > FlyingGuEntity.HOVER_RANGE * FlyingGuEntity.HOVER_RANGE) {
            approach(toEye);
            return;
        }
        bob();
    }

    private void approach(Vec3 toEye) {
        Vec3 wanted = toEye.normalize().scale(APPROACH_SPEED);
        gu.setDeltaMovement(gu.getDeltaMovement().add(wanted.subtract(gu.getDeltaMovement()).scale(EASING)));
    }

    private void bob() {
        Vec3 movement = gu.getDeltaMovement();
        double lift = Math.sin(gu.tickCount * BOB_FREQUENCY) * BOB_AMPLITUDE;
        gu.setDeltaMovement(movement.x * HOVER_DRAG, lift, movement.z * HOVER_DRAG);
    }

    private double detectRangeSqr() { return FlyingGuEntity.DETECT_RANGE * FlyingGuEntity.DETECT_RANGE; }
}
