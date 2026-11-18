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
 * 酒[Liquor]物品：作为酒虫[Liquor Worm]的食物来源，也可由玩家直接饮用。
 * <ul>
 *   <li>因需呈现饮用而非咀嚼动作才单开叶子类：通过 {@code FoodProperties} 配合 {@link UseAnim#DRINK} 动画，并附带饮用
 *       后的反胃概率。</li>
 *   <li>普通酒与四种风味酒共 5 个注册项共用本类；四种风味酒用于满足四味酒虫[Four Flavors Liquor Worm]蛊方的独立配料
 *       需求。</li>
 *   <li>⚠ {@code FoodProperties.Builder.effect} 必须传入 {@code Supplier}，否则会在注册阶段过早解析药水效果引
 *       用。</li>
 * </ul>
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
