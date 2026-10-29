package net.alex.guzhenren.item;

import net.alex.guzhenren.gameplay.aperture.Rank;
import net.alex.guzhenren.gameplay.aperture.Stage;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomModelData;

/**
 * The Advancement Icon [进度图标]: an item that exists only to carry an advancement's picture.
 *
 * <p>A vanilla advancement icon is an {@link ItemStack}, so the realm [境界] advancements show their numeral
 * through this one item. There is one picture per settable {@link Rank} and {@link Stage}: {@link Stage#NONE}
 * is the bare numeral, every other stage its stage variant. Custom model data {@code 10 × rank + stage}
 * ({@link #getModelData}; Rank III Peak is 34) picks the {@code advancement_icon_<suffix>} model and PNG
 * ({@link #getSuffix}: {@code 3}, {@code 3_peak}); a stack without it shows numeral 0. It sits in no creative
 * tab and is hidden from recipe viewers.
 *
 * <p>⚠ The PNG suffixes follow the art source ({@code aseprite-gzr/numeral/}), so the middle stage is
 * {@code _mid}, not the stage's own name. {@link #icon} refuses a rank outside the settable range, which has
 * no PNG and would silently render as 0.
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
