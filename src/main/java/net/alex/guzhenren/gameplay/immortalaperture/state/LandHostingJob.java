package net.alex.guzhenren.gameplay.immortalaperture.state;

import net.alex.guzhenren.core.Ticks;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * 福地托管任务[Land Hosting Job]：一个功能在托管轮里替主人干的活，每 {@value #INTERVAL_TICKS} 刻、有主时由组装层按顺序
 * 调一次；各任务用自己的预算，互不挤占，返回本轮处理的对象数，经营状态记在功能自己的存档段里。
 * <ul>
 *   <li>{@code owner} 是在线的主人，不在线为空（宰杀、捕获的掉落照样进库藏）。</li>
 *   <li>{@link #secondsUntilNext} 给面板倒计时用，与组装层的触发刻对齐。</li>
 * </ul>
 */

@FunctionalInterface
public interface LandHostingJob {

    int INTERVAL_TICKS = 60 * Ticks.SECOND;

    int host(@NotNull ServerLevel level, @NotNull BlissfulLandData data, @Nullable ServerPlayer owner);

    static int secondsUntilNext(@NotNull MinecraftServer server) {
        return Ticks.ceilSeconds(INTERVAL_TICKS - server.getTickCount() % INTERVAL_TICKS);
    }
}
