package net.alex.guzhenren.item.material;

import net.alex.guzhenren.gameplay.aperture.Rank;
import net.alex.guzhenren.gameplay.path.GuPath;
import net.alex.guzhenren.registry.effect.ModEffects;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.UseAnim;
import org.jetbrains.annotations.NotNull;

/**
 * 酒：酒虫[Liquor Worm]的补食，也可直接喝。
 * <ul>
 *   <li>因是喝非吃才开叶子类：{@code FoodProperties} 要 {@link UseAnim#DRINK} 动画与反胃概率。</li>
 *   <li>酒与四种风味酒共 5 个注册项共用本类；风味酒因四味酒虫[Four Flavors Liquor Worm]蛊方各要一份。</li>
 *   <li>⚠ {@code FoodProperties.Builder.effect} 须传 {@code Supplier}，否则注册期效果解析过早。</li>
 * </ul>
 *
 * @author Alex
 * @version 1.0.0
 * @see net.alex.guzhenren.item.material.GuMaterialItem
 * @since 1.0.0
 */

public class LiquorItem extends GuMaterialItem {

    private static final int NUTRITION = 1;
    private static final float SATURATION = 0.1F;
    private static final float NAUSEA_CHANCE = 0.6F;
    private static final int NAUSEA_TICKS = 300;

    public LiquorItem(Properties properties) {
        super(properties.food(createFoodProperties()), Rank.ONE, GuPath.FOOD);
    }

    private static FoodProperties createFoodProperties() {
        return new FoodProperties.Builder()
                .nutrition(NUTRITION)
                .saturationModifier(SATURATION)
                .alwaysEdible()
                .effect(() -> ModEffects.instance(MobEffects.CONFUSION, NAUSEA_TICKS), NAUSEA_CHANCE)
                .build();
    }

    @Override
    public @NotNull UseAnim getUseAnimation(@NotNull ItemStack stack) { return UseAnim.DRINK; }
}
