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
 * 自力更生蛊[Self Reliance Gu]行为：实现 {@link AutoUseBehavior}，由 {@link AutoUse} 在玩家受伤时自动触发持续自愈（
 * {@link SelfRelianceHeal}）。
 * <ul>
 *   <li>当玩家生命值低于上限 75% 时自动开启，挂载持续 {@code durationSeconds}（一至三转分别为 15/20/25 秒，另加永久兽力
 *       时长加成）的流派效果 {@code ModPathEffectTypes.SELF_RELIANCE}（载荷记录蛊虫档位），窗口期内只要生命值低于 75%
 *       即持续脉冲回血至 75%。</li>
 *   <li>窗口期内由 {@link AutoUse#tick} 每 {@link #PULSE_TICKS} tick 调用一次 {@link #pulse}，每次回复
 *       {@code pulseHeal * curve * maxHealth} 并截断至 75% 生命上限；衰减曲线 {@link #getCurve} 在生命值不高于
 *       {@link #FULL_PULSE_BELOW}（40%）时取满值 1，从 40% 到 75% 线性衰减至 {@link #FLOOR}（0.25）。</li>
 *   <li>生命值达到 75% 及以上时脉冲空转，但状态窗口照常计时；若单次治疗被外部事件取消则当次不回血，不会提前终止窗口或重
 *       新扣费；玩家濒死时由 {@link AutoUse#tick} 先行移除状态，不再触发回血。</li>
 * </ul>
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
