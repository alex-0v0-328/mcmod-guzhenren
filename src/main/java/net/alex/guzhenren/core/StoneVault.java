package net.alex.guzhenren.core;

import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;

/**
 * 能存元石的物品（元老蛊）：福地库藏页的「装入」「倒出」经它把库藏元石与背包里的元老蛊互相搬。
 * <ul>
 *   <li>放在 core 是为了让玩法层不直接引用物品层的类（顶层包不成环，见 {@code ArchitectureTest}），同 {@link Catchable}。</li>
 *   <li>{@link #isVaultReady} 为假（还没炼化）的不算；存取数都按真正搬动的块数返回。</li>
 * </ul>
 */

public interface StoneVault {

    boolean isVaultReady(@NotNull ItemStack stack);

    long capacity();

    long stored(@NotNull ItemStack stack);

    int storeStones(@NotNull ItemStack stack, int amount);

    int drawStones(@NotNull ItemStack stack, int wanted);
}
