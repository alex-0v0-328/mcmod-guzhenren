package net.alex.guzhenren.registry.block;

import net.alex.guzhenren.Guzhenren;
import net.minecraft.core.registries.Registries;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.block.Block;

/**
 * 本模组方块标签；标签键容器，非 DeferredRegister。
 * {@link #AS_BEFORE_TERRAIN}、{@link #AS_BEFORE_STRIPPED} 定山如故[Mountain As Before Gu]、
 * 江如故[River As Before Gu]复原什么：装饰改动仅在地形块互换时保留（崖壁岩层、峡谷砂岩），剥离方块（矿石、粗矿块）
 * 还原为周围石头；整合包可用数据包扩充。
 *
 * @author Alex
 * @version 1.0.0
 * @since 1.0.0
 */

public final class ModBlockTags {

    private ModBlockTags() {}

    public static final TagKey<Block> AS_BEFORE_TERRAIN = key("as_before_terrain");
    public static final TagKey<Block> AS_BEFORE_STRIPPED = key("as_before_stripped");

    private static TagKey<Block> key(String name) {
        return TagKey.create(Registries.BLOCK, Guzhenren.id(name));
    }
}
