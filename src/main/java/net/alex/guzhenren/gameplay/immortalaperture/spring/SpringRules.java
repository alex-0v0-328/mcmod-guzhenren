package net.alex.guzhenren.gameplay.immortalaperture.spring;

import net.alex.guzhenren.core.Ticks;
import net.alex.guzhenren.gameplay.immortalaperture.state.LandHostingJob;

/**
 * 福地元泉[Spirit Spring]收集的共用数字：元泉方块的产石节拍与地灵每轮的结算量，两端都读（服务端结算，客户端总览卡）。
 * <ul>
 *   <li>产石节拍 {@link #PRODUCTION_INTERVAL_TICKS} 与每次 {@link #STONES_PER_PRODUCTION} 颗是元泉方块的原值，由方块沿用；
 *       福地里每眼泉每轮托管折算同样的产量：{@link #PRODUCTIONS_PER_ROUND} 次 × 每次颗数 =
 *       {@link #STONES_PER_SPRING_PER_ROUND}，不设上限。</li>
 *   <li>{@link #stonesPerRound} 按眼数算一轮的总颗数，{@link #stonesPerMinute} 同一轮折成每分钟（托管轮正好 60 秒）。</li>
 * </ul>
 */

public final class SpringRules {

    private SpringRules() {}

    public static final int PRODUCTION_INTERVAL_TICKS = 5 * Ticks.SECOND;
    public static final int STONES_PER_PRODUCTION = 64;
    public static final int PRODUCTIONS_PER_ROUND = LandHostingJob.INTERVAL_TICKS / PRODUCTION_INTERVAL_TICKS;
    public static final int STONES_PER_SPRING_PER_ROUND = PRODUCTIONS_PER_ROUND * STONES_PER_PRODUCTION;

    public static long stonesPerRound(int springs) { return (long) Math.max(0, springs) * STONES_PER_SPRING_PER_ROUND; }

    public static long stonesPerMinute(int springs) {
        return stonesPerRound(springs) * Ticks.MINUTE / LandHostingJob.INTERVAL_TICKS;
    }
}
