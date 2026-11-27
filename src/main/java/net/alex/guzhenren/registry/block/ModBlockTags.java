package net.alex.guzhenren.registry.block;

import net.alex.guzhenren.Guzhenren;
import net.minecraft.core.registries.Registries;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.block.Block;

/**
 * 本模组方块标签键容器（非 {@code DeferredRegister}）：{@link #AS_BEFORE_TERRAIN} 与 {@link #AS_BEFORE_STRIPPED} 共同界
 * 定山如故[Mountain As Before Gu]和江如故[River As Before Gu]的地形复原边界（前者允许在地形方块互换时保留装饰性改动如崖
 * 壁岩层与峡谷砂岩，后者使矿石、粗矿块等被剥离方块还原为周围石头），整合包可通过数据包自由扩充；
 * {@link #MORTAL_AS_BEFORE_IMMUNE} 收录凡蛊如故还原不动的方块（荡魂山石、胆石），连同其正下方连续的支撑方块一起跳过；
 * {@link #NOT_GROUND} 收录有碰撞箱但又薄又小、福地找真实地面时照样跳过的方块（地毯、多层雪、灯笼、头颅等）。
 */

public final class ModBlockTags {

    private ModBlockTags() {}

    public static final TagKey<Block> AS_BEFORE_TERRAIN = key("as_before_terrain");
    public static final TagKey<Block> AS_BEFORE_STRIPPED = key("as_before_stripped");
    public static final TagKey<Block> MORTAL_AS_BEFORE_IMMUNE = key("mortal_as_before_immune");
    public static final TagKey<Block> NOT_GROUND = key("not_ground");

    private static TagKey<Block> key(String name) {
        return TagKey.create(Registries.BLOCK, Guzhenren.id(name));
    }
}
