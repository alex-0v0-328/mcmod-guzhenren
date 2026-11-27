package net.alex.guzhenren.registry.item;

import java.util.EnumMap;
import java.util.List;
import net.alex.guzhenren.Guzhenren;
import net.minecraft.Util;
import net.minecraft.core.registries.Registries;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.crafting.Ingredient;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * 本模组护甲材质注册表。
 * <ul>
 *   <li>{@link #SCHOOL_UNIFORM} 对齐原版皮革甲数值（胸 3 / 腿 2 / 鞋 1，附魔值 15，无韧性与击退抗性，羊毛修复）；
 *       声明可染色主布料层与固定细节层两层 {@link ArmorMaterial.Layer}，供 Epic Fight {@code WearableItemLayer}
 *       在制服蒙皮网格上逐层绘制并与 GeckoLib 双层渲染保持一致。</li>
 * </ul>
 */

public final class ModArmorMaterials {

    private ModArmorMaterials() {}

    public static final DeferredRegister<ArmorMaterial> ARMOR_MATERIALS =
            DeferredRegister.create(Registries.ARMOR_MATERIAL, Guzhenren.MOD_ID);

    public static final int SCHOOL_UNIFORM_DURABILITY_MULTIPLIER = 5;

    public static final DeferredHolder<ArmorMaterial, ArmorMaterial> SCHOOL_UNIFORM = ARMOR_MATERIALS.register(
            "school_uniform", () -> new ArmorMaterial(
                    Util.make(new EnumMap<>(ArmorItem.Type.class), map -> {
                        map.put(ArmorItem.Type.BOOTS, 1);
                        map.put(ArmorItem.Type.LEGGINGS, 2);
                        map.put(ArmorItem.Type.CHESTPLATE, 3);
                        map.put(ArmorItem.Type.HELMET, 1);
                        map.put(ArmorItem.Type.BODY, 3);
                    }),
                    15,
                    SoundEvents.ARMOR_EQUIP_LEATHER,
                    () -> Ingredient.of(ItemTags.WOOL),
                    List.of(
                            new ArmorMaterial.Layer(Guzhenren.id("school_uniform"), "", true),
                            new ArmorMaterial.Layer(Guzhenren.id("school_uniform"), "_base", false)),
                    0.0F,
                    0.0F));

    public static void register(IEventBus modEventBus) {
        ARMOR_MATERIALS.register(modEventBus);
    }
}
