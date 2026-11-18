package net.alex.guzhenren.effect.timed;

import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;

/**
 * 横冲蛊[Horizontal Crash Gu]、直撞蛊[Vertical Crash Gu]与横冲直撞蛊[Charging Crash Gu]共用的效果标识类。
 * <ul>
 *   <li>客户端依据身上持有的效果注册项判定可冲刺轴向（横冲开放水平轴，直撞开放竖直轴，横冲直撞同时开放两轴，跨四、五转
 *       时按转数显示不同图标），并通过 {@link net.alex.guzhenren.network.payload.DashPayload} 上报冲刺意图。</li>
 *   <li>冲刺距离与动画播放速度规格定义在 {@link net.alex.guzhenren.gameplay.path.strength.CrashDash} 中，以便纯 JVM 单
 *       元测试在不加载 {@link MobEffect} 的环境下直接验证。</li>
 * </ul>
 */

public final class CrashGuEffect extends MobEffect {

    public CrashGuEffect(MobEffectCategory category, int color) {
        super(category, color);
    }
}
