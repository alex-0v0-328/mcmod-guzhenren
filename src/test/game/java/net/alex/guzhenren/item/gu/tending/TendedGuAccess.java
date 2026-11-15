package net.alex.guzhenren.item.gu.tending;

import net.minecraft.world.item.ItemStack;

/**
 * 测试用的 {@link TendedGuItem} 受保护钩子入口，供同读 {@code MortalGuItem} 受保护属性的包外测试。
 *
 * @author Alex
 * @version 1.0.0
 * @since 1.0.0
 */
public final class TendedGuAccess {

    private TendedGuAccess() {}

    public static long useThreshold(TendedGuItem gu, ItemStack stack) { return gu.useThreshold(stack); }

    public static boolean canUseDuringEffectCooldown(TendedGuItem gu) { return gu.canUseDuringEffectCooldown(); }
}
