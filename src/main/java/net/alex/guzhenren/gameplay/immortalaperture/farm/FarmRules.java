package net.alex.guzhenren.gameplay.immortalaperture.farm;

import java.util.List;
import net.alex.guzhenren.core.Ticks;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;

/**
 * 福地种植的共用规则：两端都读（服务端开垦、托管，客户端种植页与提示）。
 */

public final class FarmRules {

    private FarmRules() {}

    public static final int RECLAIM_COOLDOWN_TICKS = 60 * Ticks.SECOND;
    public static final int HOSTING_FIELDS = 8;
    public static final List<Block> CROPS = List.of(Blocks.WHEAT, Blocks.CARROTS, Blocks.POTATOES, Blocks.BEETROOTS);
}
