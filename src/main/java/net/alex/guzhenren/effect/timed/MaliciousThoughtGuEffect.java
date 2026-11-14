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
 * 恶念蛊[Malicious Thought Gu]效果：12 秒内每秒给脑海加恶念。
 * <ul>
 *   <li>投影流派效果 {@code StateEffects.grantThought}（真值）：显示时脉冲，{@code /effect clear} 后 1 秒内补回。</li>
 *   <li>四档：倍率即转数档位，{@code evilPerSecond} 注册时给定；即时部分归蛊产出，本效果只管每秒滴加。</li>
 * </ul>
 *
 * @author Alex
 * @version 1.0.0
 * @see MindService
 * @since 1.0.0
 */

public class MaliciousThoughtGuEffect extends PeriodicEffect {

    public static final int DURATION_TICKS = 12 * Ticks.SECOND;
    private final long[] evilPerSecond;

    public MaliciousThoughtGuEffect(MobEffectCategory category, int color, long[] evilPerSecond) {
        super(category, color, Ticks.SECOND);
        this.evilPerSecond = evilPerSecond;
    }

    @Override
    protected void onPulse(@NotNull LivingEntity entity, int amplifier) {
        if (entity instanceof ServerPlayer player) {
            int index = Mth.clamp(amplifier, 0, evilPerSecond.length - 1);
            MindService.addThoughts(player, evilPerSecond[index], ThoughtTag.EVIL);
        }
    }
}
