package net.alex.guzhenren.item.gu.mortal.path.strength;

import net.alex.guzhenren.gameplay.path.PathEffect;
import net.alex.guzhenren.gameplay.path.PathEffectService;
import net.alex.guzhenren.gameplay.path.PathEffectType;
import net.alex.guzhenren.gameplay.path.strength.HardshipStrength;
import net.alex.guzhenren.gameplay.path.strength.SelfRelianceHeal;
import net.alex.guzhenren.item.gu.MortalGuItem;
import net.alex.guzhenren.registry.effect.ModPathEffectTypes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;

/**
 * The Self Reliance behavior [自力更生]: an auto heal ({@link AutoUseBehavior}) that starts under 75% of max health
 * and mends the holder back up to it. The state is the permanent path effect {@code ModPathEffectTypes.SELF_RELIANCE},
 * its payload the Gu's grade.
 *
 * <p>Every {@link #PULSE_TICKS} ({@link #pulse}, from {@link AutoUse#tick}) the heal mends
 * {@code pulseHeal × curve × max health}, never past 75%; the last pulse is cut there and the heal stops. The curve
 * ({@link #getCurve}) is 1 at or under {@link #FULL_PULSE_BELOW} of max health and falls linearly to {@link #FLOOR}
 * at 75%. Damage taken meanwhile is healed too.
 *
 * <p>A pulse that a cancelled heal leaves without effect pauses the heal; it neither ends it nor bills again. A
 * dying body is never healed: {@link AutoUse#tick} stops the state first.
 *
 * @author Alex
 * @version 1.0.0
 * @see AutoUse
 * @see SelfRelianceHeal
 * @since 1.0.0
 */

public record SelfRelianceBehavior(int grade, float pulseHeal) implements AutoUseBehavior {

    public static final int PULSE_TICKS = 5;
    public static final float FULL_PULSE_BELOW = 0.40F;
    public static final float FLOOR = 0.25F;

    @Override
    public @NotNull PathEffectType<?> getStateType() { return ModPathEffectTypes.SELF_RELIANCE; }

    @Override
    public boolean canStart(@NotNull ServerPlayer player) { return true; }

    @Override
    public void applyEffect(MortalGuItem gu, ServerPlayer player, ItemStack stack) {
        PathEffectService.apply(player, gu.createEffect(ModPathEffectTypes.SELF_RELIANCE, PathEffect.PERMANENT,
                new SelfRelianceHeal(grade)));
    }

    public static void pulse(@NotNull ServerPlayer player) {
        if (player.tickCount % PULSE_TICKS != 0) return;

        PathEffect<SelfRelianceHeal> heal = PathEffectService.find(player, ModPathEffectTypes.SELF_RELIANCE);
        if (heal == null) return;

        SelfRelianceBehavior behavior = BuiltInRegistries.ITEM.get(heal.source()) instanceof MortalGuItem gu
                ? gu.getBehavior(SelfRelianceBehavior.class) : null;
        if (behavior != null) behavior.mend(player);
        if (behavior == null || !AutoUse.isHurt(player)) {
            PathEffectService.removeType(player, ModPathEffectTypes.SELF_RELIANCE);
        }
    }

    public static float getCurve(float health, float maxHealth) {
        float full = maxHealth * FULL_PULSE_BELOW;
        if (health <= full) return 1.0F;

        float fall = Math.min(1.0F, (health - full) / (maxHealth * HardshipStrength.START_BELOW - full));
        return 1.0F - fall * (1.0F - FLOOR);
    }

    private void mend(ServerPlayer player) {
        float max = player.getMaxHealth();
        float health = player.getHealth();
        player.heal(Math.min(pulseHeal * getCurve(health, max) * max, max * HardshipStrength.START_BELOW - health));
    }
}
