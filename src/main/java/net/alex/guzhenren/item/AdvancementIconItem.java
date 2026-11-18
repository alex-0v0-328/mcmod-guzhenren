package net.alex.guzhenren.item;

import net.alex.guzhenren.gameplay.aperture.Rank;
import net.alex.guzhenren.gameplay.aperture.Stage;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomModelData;

/**
 * 进度图标物品：专供原版进度界面承载境界数字图标的纯表现物品。
 * <ul>
 *   <li>原版进度图标只能绑定 {@link ItemStack}，因此所有境界进度的数字图标均通过本物品展示；每个有效的 {@link Rank} 与
 *       {@link Stage} 组合对应一张贴图（{@link Stage#NONE} 为纯数字），物品本身不进入创造模式标签页并对配方查看器隐
 *       藏。</li>
 *   <li>自定义模型数据按 {@code 10 * rank + stage} 编码（{@link #getModelData}，如三转巅峰为 {@code 34}），用于在模型覆
 *       盖表中选择 {@code advancement_icon_<suffix>} 模型与对应 PNG（{@link #getSuffix}，如
 *       {@code 3}、{@code 3_peak}），未匹配时回退显示 0。</li>
 *   <li>⚠ PNG 后缀遵循美术源目录 {@code aseprite-gzr/numeral/} 的命名，中阶固定为 {@code _mid} 而非枚举
 *       名；{@link #icon} 会主动拒绝超出凡人范围的转数，防止因缺少对应 PNG 而静默回退渲染为 0。</li>
 * </ul>
 */

public final class AdvancementIconItem extends Item {

    private static final int MODEL_DATA_PER_RANK = 10;

    public AdvancementIconItem(Properties properties) {
        super(properties);
    }

    public ItemStack icon(Rank rank, Stage stage) {
        if (rank.compareTo(Rank.LOWEST) < 0 || rank.compareTo(Rank.HIGHEST) > 0) {
            throw new IllegalArgumentException("no advancement icon for " + rank);
        }
        ItemStack stack = new ItemStack(this);
        stack.set(DataComponents.CUSTOM_MODEL_DATA, new CustomModelData(getModelData(rank, stage)));
        return stack;
    }

    public static int getModelData(Rank rank, Stage stage) {
        return MODEL_DATA_PER_RANK * rank.ordinal() + stage.ordinal();
    }

    public static String getSuffix(Rank rank, Stage stage) {
        String numeral = String.valueOf(rank.ordinal());
        return switch (stage) {
            case NONE -> numeral;
            case INIT -> numeral + "_init";
            case MIDDLE -> numeral + "_mid";
            case UPPER -> numeral + "_upper";
            case PEAK -> numeral + "_peak";
        };
    }
}
