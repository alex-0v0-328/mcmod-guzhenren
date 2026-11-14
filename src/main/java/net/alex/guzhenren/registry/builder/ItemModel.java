package net.alex.guzhenren.registry.builder;

/**
 * 已声明物品的模型种类：{@code ModItemModelProvider} 会写的形状。
 *
 * <ul>
 *   <li>{@link #FLAT} 默认，原版 {@code item/generated} 叠 {@code item/<id>}；{@link #BLOCK} 同 ID 方块模型；
 *       {@link #FLUID} 平放 {@code block/<id>_still}，因元泉[Spirit Spring] BlockItem 无自带 PNG。</li>
 *   <li>{@link #LEAF_STAGES}：九叶生机草[Nine Leaf Vitality Grass] {@code _full}，按 {@code picked} 切 {@code _half}、
 *       {@code _empty}；{@link #NUMERALS}：进度图标 {@code _0}，按 {@code custom_model_data} 切换，每个可设的转、
 *       阶一个数字。</li>
 *   <li>种类是名字而非回调，注册行从不引用客户端 datagen 类。</li>
 * </ul>
 *
 * @author Alex
 * @version 1.0.0
 * @see ItemBuilder#model(ItemModel)
 * @since 1.0.0
 */

public enum ItemModel { FLAT, BLOCK, FLUID, LEAF_STAGES, NUMERALS }
