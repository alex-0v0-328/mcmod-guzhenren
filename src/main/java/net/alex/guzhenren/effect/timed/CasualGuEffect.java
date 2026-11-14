package net.alex.guzhenren.effect.timed;

import net.alex.guzhenren.core.Ticks;
import net.alex.guzhenren.effect.PeriodicEffect;
import net.alex.guzhenren.gameplay.mind.MindService;
import net.alex.guzhenren.gameplay.mind.ThoughtTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.LivingEntity;
import org.jetbrains.annotations.NotNull;

/**
 * 随意蛊[Casual Gu]效果：10 秒内每秒给脑海加随机自然念。
 * <ul>
 *   <li>投影流派效果 {@code StateEffects.grantThought}（真值）：显示时脉冲，{@code /effect clear} 后 1 秒内补回。</li>
 *   <li>两档：倍率即转数档位，{@code RANGES} 逐档加宽。</li>
 * </ul>
 *
 * @author Alex
 * @version 1.0.0
 * @see MindService
 * @since 1.0.0
 */

public class CasualGuEffect extends PeriodicEffect {

    public static final int DURATION_TICKS = 10 * Ticks.SECOND;
    private static final int[][] RANGES = { { 1, 100 }, { 100, 200 } };

    public CasualGuEffect(MobEffectCategory category, int color) {
        super(category, color, Ticks.SECOND);
    }

    @Override
    protected void onPulse(@NotNull LivingEntity entity, int amplifier) {
        if (entity instanceof ServerPlayer player) {
            int index = Mth.clamp(amplifier, 0, RANGES.length - 1);
            int amount = player.getRandom().nextIntBetweenInclusive(RANGES[index][0], RANGES[index][1]);
            MindService.addThoughts(player, amount, ThoughtTag.NATURAL);
        }
    }
}
