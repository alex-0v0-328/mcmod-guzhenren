package net.alex.guzhenren.gameplay.immortalaperture.farm;

import java.util.List;
import net.alex.guzhenren.registry.block.ModBlocks;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import org.jetbrains.annotations.NotNull;

/**
 * 福地种植的共用规则：两端都读（服务端开垦、托管，客户端种植页、地图选区卡与操作栏、提示）。
 * <ul>
 *   <li>{@link #crops()} 含模组方块，首次调用（注册完成后）才建表，类加载早于注册也不出错。</li>
 * </ul>
 */

public final class FarmRules {

    private FarmRules() {}

    public static final int HOSTING_FIELDS = 8;

    public static @NotNull List<Block> crops() { return Crops.ALL; }

    private static final class Crops {

        private static final List<Block> ALL = List.of(Blocks.WHEAT, Blocks.CARROTS, Blocks.POTATOES,
                Blocks.BEETROOTS, ModBlocks.INTIMATE_GRASS_CROP.get());
    }
}
