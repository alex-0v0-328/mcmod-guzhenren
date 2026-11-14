package net.alex.guzhenren.item;

import net.alex.guzhenren.gameplay.aperture.Rank;
import net.alex.guzhenren.gameplay.aperture.Stage;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomModelData;

/**
 * 进度图标：只为承载进度图片而存在的物品。
 * <ul>
 *   <li>原版进度图标是 {@link ItemStack}，境界进度的数字都经本物品显示；每个可设定 {@link Rank}、{@link Stage} 一张图，
 *       {@link Stage#NONE} 为光数字；不进创造标签页，对配方查看器隐藏。</li>
 *   <li>模型数据 {@code 10 × rank + stage}（{@link #getModelData}，三转巅峰 = 34）选 {@code advancement_icon_<suffix>}
 *       模型与 PNG（{@link #getSuffix}：{@code 3}、{@code 3_peak}），无则显示 0。</li>
 *   <li>⚠ PNG 后缀随美术源 {@code aseprite-gzr/numeral/}，中阶为 {@code _mid} 而非阶段名；{@link #icon} 拒绝范围外转
 *       数：无 PNG，会静默渲染成 0。</li>
 * </ul>
 *
 * @author Alex
 * @version 1.0.0
 * @since 1.0.0
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
