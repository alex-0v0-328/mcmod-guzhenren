package net.alex.guzhenren.registry.builder;

/**
 * 声明式物品模型形状枚举（供 {@link ItemBuilder#model(ItemModel)} 指定并由 {@code ModItemModelProvider} 生成对应模型
 * JSON），以轻量名字解耦注册期代码与客户端 datagen 类。
 * <ul>
 *   <li>{@link #FLAT} 为默认原版 {@code item/generated} 叠 {@code item/<id>} 贴图；{@link #BLOCK} 直接继承同名方块模
 *       型；{@link #FLUID} 平铺 {@code block/<id>_still} 流体条带（专用于无独立物品贴图的元泉[Spirit Spring]
 *       {@code BlockItem}）。</li>
 *   <li>{@link #LEAF_STAGES} 为九叶生机草[Nine Leaf Vitality Grass]按 {@code picked} 属性在 {@code _full}、
 *       {@code _half} 与 {@code _empty} 间切换；{@link #NUMERALS} 为进度数字图标按 {@code custom_model_data} 在
 *       {@code _0} 起各转阶编号模型间切换。</li>
 *   <li>{@link #SERIES} 为成套物品（如分节古籍 {@code renzu_zhuan_<n>}）共用去掉末尾 {@code _<序号>} 的同一张贴
 *       图。</li>
 * </ul>
 */

public enum ItemModel { FLAT, LAYERED, BLOCK, FLUID, LEAF_STAGES, NUMERALS, SERIES }
