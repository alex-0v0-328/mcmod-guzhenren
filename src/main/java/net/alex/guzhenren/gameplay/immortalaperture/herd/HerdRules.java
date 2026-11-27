package net.alex.guzhenren.gameplay.immortalaperture.herd;

import net.alex.guzhenren.core.Ticks;

/**
 * 福地养殖与牧场的共用规则：两端都读（服务端繁殖、宰杀、收拢、托管，客户端养殖页与牧场卡片）。
 */

public final class HerdRules {

    private HerdRules() {}

    public static final int LIMIT = 64;
    public static final int HOSTING_ANIMALS = 32;
    public static final int GATHER_LIMIT = 16;
    public static final int GATHER_COOLDOWN_TICKS = 60 * Ticks.SECOND;
}
