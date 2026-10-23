package net.alex.guzhenren.item.material;

import net.alex.guzhenren.gameplay.aperture.Rank;
import net.alex.guzhenren.gameplay.path.GuPath;
import net.alex.guzhenren.registry.effect.ModEffects;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.UseAnim;
import org.jetbrains.annotations.NotNull;

/**
 * Liquor [酒]: what a Liquor Worm [酒虫] is fed, and a drink in its own right.
 *
 * <p>Extends {@link net.alex.guzhenren.item.material.GuMaterialItem}. It earns a leaf class only because
 * it is drunk rather than eaten -- the {@code FoodProperties} builder needs a
 * {@link net.minecraft.world.item.UseAnim#DRINK} animation and a nausea chance. Five registrations (酒 plus
 * four flavors) use this one class; the flavors exist because the 四味酒虫 recipe takes one of each.
 *
 * <p>⚠ {@code FoodProperties.Builder.effect} must take a {@code Supplier} -- the effect holder resolves
 * too early during registration otherwise.
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
                .effect(() -> ModEffects.instance(ModEffects.VANILLA_CONFUSION, NAUSEA_TICKS), NAUSEA_CHANCE)
                .build();
    }

    @Override
    public @NotNull UseAnim getUseAnimation(@NotNull ItemStack stack) { return UseAnim.DRINK; }
}
