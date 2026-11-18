package net.alex.guzhenren.registry.block;

import net.alex.guzhenren.Guzhenren;
import net.minecraft.core.registries.Registries;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.block.Block;

/**
 * 本模组方块标签键容器（非 {@code DeferredRegister}）：{@link #AS_BEFORE_TERRAIN} 与 {@link #AS_BEFORE_STRIPPED} 共同界
 * 定山如故[Mountain As Before Gu]和江如故[River As Before Gu]的地形复原边界（前者允许在地形方块互换时保留装饰性改动如崖
 * 壁岩层与峡谷砂岩，后者使矿石、粗矿块等被剥离方块还原为周围石头），整合包可通过数据包自由扩充。
 */

public final class ModBlockTags {

    private ModBlockTags() {}

    public static final TagKey<Block> AS_BEFORE_TERRAIN = key("as_before_terrain");
    public static final TagKey<Block> AS_BEFORE_STRIPPED = key("as_before_stripped");

    private static TagKey<Block> key(String name) {
        return TagKey.create(Registries.BLOCK, Guzhenren.id(name));
    }
}
