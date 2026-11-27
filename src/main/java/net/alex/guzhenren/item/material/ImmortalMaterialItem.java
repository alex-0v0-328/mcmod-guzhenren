package net.alex.guzhenren.item.material;

import java.util.List;
import net.alex.guzhenren.gameplay.aperture.Rank;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import org.jetbrains.annotations.NotNull;

/**
 * 仙材[Immortal Material]：六至九转材料，只带转数、不分流派，悬停一行「转数 + 仙材」；暂无用途。
 * <ul>
 *   <li>不继承 {@code GuItem}：蛊虫与蛊材必有流派，仙元[Immortal Essence]出自天地人三气调和而无流派（Alex 定）。</li>
 * </ul>
 */

public class ImmortalMaterialItem extends Item {

    private static final String KIND_KEY = "guzhenren.display.immortal_material";
    private final Rank rank;

    public ImmortalMaterialItem(Properties properties, Rank rank) {
        super(properties);
        this.rank = rank;
    }

    public Rank getRank() { return rank; }

    @Override
    public void appendHoverText(@NotNull ItemStack stack, @NotNull TooltipContext context,
                                @NotNull List<Component> tooltip, @NotNull TooltipFlag flag) {
        tooltip.add(Component.translatable(rank.getTranslationKey()).append(Component.translatable(KIND_KEY))
                .withStyle(ChatFormatting.GRAY));
    }
}
