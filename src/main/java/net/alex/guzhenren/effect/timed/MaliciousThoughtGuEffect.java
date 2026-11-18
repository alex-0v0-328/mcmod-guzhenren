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
 * 恶念蛊[Malicious Thought Gu]的周期效果：在 12 秒内每秒按注册时给定的 {@code evilPerSecond} 配列向脑海注入恶念（真值由
 * 流派效果 {@code StateEffects.grantThought} 持有并负责在被清除后 1 秒内补回）。效果倍率对应四转以内的转数档位，即时产
 * 出的念头由蛊虫使用逻辑处理，本类仅负责每秒持续滴加并调用 {@link MindService} 写入。
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
