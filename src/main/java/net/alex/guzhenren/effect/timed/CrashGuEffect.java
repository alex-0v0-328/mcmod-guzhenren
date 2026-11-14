package net.alex.guzhenren.effect.timed;

import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;

/**
 * 横冲蛊[Horizontal Crash Gu]、直撞蛊[Vertical Crash Gu]、横冲直撞蛊[Charging Crash Gu]共用效果类。
 * <ul>
 *   <li>横冲开横轴，直撞开纵轴，横冲直撞两轴；客户端按身上效果的注册项定可用轴，位移经
 *       {@link net.alex.guzhenren.network.payload.DashPayload} 上报；横冲直撞跨四、五转，图标按转分档。</li>
 *   <li>距离规格在 {@link CrashDash}，因 {@code MobEffect} 纯 JUnit 载不了。</li>
 * </ul>
 *
 * @author Alex
 * @version 1.0.0
 * @see net.alex.guzhenren.network.payload.DashPayload
 * @since 1.0.0
 */

public final class CrashGuEffect extends MobEffect {

    public CrashGuEffect(MobEffectCategory category, int color) {
        super(category, color);
    }
}
