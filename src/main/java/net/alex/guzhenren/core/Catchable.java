package net.alex.guzhenren.core;

import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;

/**
 * 能被整只捕获成物品的实体：野生蛊右键捕获与福地「蛊虫」页捕获入库共用同一份物品。
 * <ul>
 *   <li>放在 core 是为了让玩法层只认这个接口和实体类型标签，不直接引用实体层的类（顶层包不成环，见
 *       {@code ArchitectureTest}）。</li>
 * </ul>
 */

public interface Catchable {

    @NotNull ItemStack caughtStack();
}
