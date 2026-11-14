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
 * 自力更生蛊[Self Reliance Gu]的行为：自动治疗（{@link AutoUseBehavior}）。
 * <ul>
 *   <li>低于最大生命 75% 开启；{@code durationSeconds} 窗口（按转 15/20/25 秒）内低于 75% 就补回 75%，期间受伤也补；
 *       状态为限时流派效果 {@code ModPathEffectTypes.SELF_RELIANCE}，payload 为蛊的档位。</li>
 *   <li>每 {@link #PULSE_TICKS}（{@link AutoUse#tick} 调 {@link #pulse}）回 {@code pulseHeal × curve × max health}，
 *       截在 75%；曲线在 {@link #FULL_PULSE_BELOW} 及以下为 1，到 75% 线性降至 {@link #FLOOR}。</li>
 *   <li>75% 及以上脉冲无效，窗口照走；治疗被取消则暂停，不结束不重计费；濒死不治，{@link AutoUse#tick} 先停状态。</li>
 * </ul>
 *
 * @author Alex
 * @version 1.0.0
 * @see AutoUse
 * @see SelfRelianceHeal
 * @since 1.0.0
 */

public record SelfRelianceBehavior(int grade, float pulseHeal, int durationSeconds) implements AutoUseBehavior {

    public static final int PULSE_TICKS = 5;
    public static final float FULL_PULSE_BELOW = 0.40F;
    public static final float FLOOR = 0.25F;

    @Override
    public @NotNull PathEffectType<?> getStateType() { return ModPathEffectTypes.SELF_RELIANCE; }

    @Override
    public boolean canStart(@NotNull ServerPlayer player) { return true; }

    @Override
    public int getDurationSeconds() { return durationSeconds; }

    @Override
    public void applyEffect(MortalGuItem gu, ServerPlayer player, ItemStack stack) {
        PathEffectService.apply(player, gu.createEffect(ModPathEffectTypes.SELF_RELIANCE,
                AutoUse.getEndTick(player, this), new SelfRelianceHeal(grade)));
    }

    public static void pulse(@NotNull ServerPlayer player) {
        if (player.tickCount % PULSE_TICKS != 0) return;

        PathEffect<SelfRelianceHeal> heal = PathEffectService.find(player, ModPathEffectTypes.SELF_RELIANCE);
        if (heal == null) return;

        SelfRelianceBehavior behavior = BuiltInRegistries.ITEM.get(heal.source()) instanceof MortalGuItem gu
                ? gu.getBehavior(SelfRelianceBehavior.class) : null;
        if (behavior == null) {
            PathEffectService.removeType(player, ModPathEffectTypes.SELF_RELIANCE);
        } else if (AutoUse.isHurt(player)) {
            behavior.mend(player);
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
