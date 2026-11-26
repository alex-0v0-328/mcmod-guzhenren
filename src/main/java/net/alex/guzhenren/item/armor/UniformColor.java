package net.alex.guzhenren.item.armor;

import java.util.EnumMap;
import java.util.Map;
import java.util.Objects;
import net.alex.guzhenren.registry.item.ModDataComponents;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.ItemStack;

/**
 * 学堂制服[School Uniform]的 16 色低饱和古风重映射表与物品染色读写工具。
 * <ul>
 *   <li>原版 16 种 {@link DyeColor} 逐一映射到低饱和做旧古风 RGB 色值（非自由混色），默认色为玄墨炭黑
 *       （{@link #DEFAULT}，{@code #2D313C}）。</li>
 *   <li>物品堆上的默认色不写入 {@link ModDataComponents#UNIFORM_COLOR}（写默认色等价于移除组件），保证创造栏取
 *       出、洗色还原与显式染黑后的物品堆数据一致。</li>
 * </ul>
 */

public final class UniformColor {

    public static final DyeColor DEFAULT = DyeColor.BLACK;
    private static final Map<DyeColor, Integer> PALETTE = buildPalette();

    private UniformColor() {}

    private static Map<DyeColor, Integer> buildPalette() {
        Map<DyeColor, Integer> map = new EnumMap<>(DyeColor.class);
        map.put(DyeColor.WHITE, 0xD4CCB8);
        map.put(DyeColor.LIGHT_GRAY, 0x9DA3A6);
        map.put(DyeColor.GRAY, 0x5E666E);
        map.put(DyeColor.BLACK, 0x2D313C);
        map.put(DyeColor.BROWN, 0x6B5442);
        map.put(DyeColor.RED, 0x82423D);
        map.put(DyeColor.ORANGE, 0x9E6846);
        map.put(DyeColor.YELLOW, 0xB39A62);
        map.put(DyeColor.LIME, 0x73825B);
        map.put(DyeColor.GREEN, 0x3F5D50);
        map.put(DyeColor.CYAN, 0x446B6E);
        map.put(DyeColor.LIGHT_BLUE, 0x6B859C);
        map.put(DyeColor.BLUE, 0x445E82);
        map.put(DyeColor.PURPLE, 0x5F537D);
        map.put(DyeColor.MAGENTA, 0x7A4F66);
        map.put(DyeColor.PINK, 0x9E787C);
        return Map.copyOf(map);
    }

    public static int rgb(DyeColor color) {
        return PALETTE.get(Objects.requireNonNull(color));
    }

    public static int argb(DyeColor color) {
        return 0xFF000000 | rgb(color);
    }

    public static String translationKey(DyeColor color) {
        return "guzhenren.uniform_color." + Objects.requireNonNull(color).getName();
    }

    public static DyeColor getColor(ItemStack stack) {
        DyeColor stored = stack.get(ModDataComponents.UNIFORM_COLOR.get());
        return stored != null ? stored : DEFAULT;
    }

    public static boolean isDefault(ItemStack stack) {
        return getColor(stack) == DEFAULT;
    }

    public static void setColor(ItemStack stack, DyeColor color) {
        Objects.requireNonNull(color);
        if (color == DEFAULT) {
            stack.remove(ModDataComponents.UNIFORM_COLOR.get());
        } else {
            stack.set(ModDataComponents.UNIFORM_COLOR.get(), color);
        }
    }
}
