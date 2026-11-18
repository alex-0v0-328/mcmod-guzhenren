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
 * 随意蛊[Casual Gu]的周期效果：在 10 秒内每秒为玩家脑海补充随机数量的自然念（真值由流派效果
 * {@code StateEffects.grantThought} 持有并负责在被清除后 1 秒内补回）。当前共两档，效果倍率对应转数档位，每次脉冲由
 * {@link MindService} 按 {@code RANGES} 区间抽取数量写入。
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
