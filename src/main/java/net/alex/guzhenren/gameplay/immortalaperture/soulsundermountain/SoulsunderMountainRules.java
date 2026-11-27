package net.alex.guzhenren.gameplay.immortalaperture.soulsundermountain;

import net.alex.guzhenren.core.Ticks;

/**
 * 荡魂山页操作的共用规则：两端都读（服务端挪生物上山，客户端秘境页）。
 */

public final class SoulsunderMountainRules {

    private SoulsunderMountainRules() {}

    public static final int MOVE_LIMIT = 64;
    public static final int MOVE_ATTEMPTS = 16;
    public static final int MOVE_COOLDOWN_TICKS = 60 * Ticks.SECOND;
}
