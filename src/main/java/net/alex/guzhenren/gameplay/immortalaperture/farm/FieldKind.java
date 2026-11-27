package net.alex.guzhenren.gameplay.immortalaperture.farm;

import net.alex.guzhenren.registry.block.ModBlocks;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import org.jetbrains.annotations.NotNull;

/**
 * 标准田的田型：普通田铺原版耕地，蛊植田铺黑土耕地（掺腐泥冻土，作物快 4 倍）；两种田能种的作物相同。
 * <ul>
 *   <li>存档与网络都记 {@link #ordinal()}，{@link #byId} 越界按普通田。</li>
 * </ul>
 */

public enum FieldKind {
    PLAIN,
    GU_PLANT;

    public static @NotNull FieldKind byId(int id) { return id == GU_PLANT.ordinal() ? GU_PLANT : PLAIN; }

    public @NotNull Block soil() { return this == GU_PLANT ? ModBlocks.BLACK_SOIL_FARMLAND.get() : Blocks.FARMLAND; }
}
