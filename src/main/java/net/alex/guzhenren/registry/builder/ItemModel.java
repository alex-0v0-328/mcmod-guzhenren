package net.alex.guzhenren.registry.builder;

/**
 * The item model a declared item gets: the shapes {@code ModItemModelProvider} knows how to write.
 *
 * <p>{@link #FLAT} is the default, vanilla's {@code item/generated} over {@code item/<id>}. {@link #BLOCK} shows the
 * block model of the same id; {@link #FLUID} lays the fluid's still strip {@code block/<id>_still} flat, the Spirit
 * Spring [元泉] BlockItem having no PNG of its own; {@link #LEAF_STAGES} shows the Nine Leaf Vitality Grass's
 * [九叶生机草] {@code _full} PNG and overrides to {@code _half} and {@code _empty} on its {@code picked} property;
 * {@link #NUMERALS} shows the Advancement Icon's [进度图标] {@code _0} PNG and overrides on
 * {@code custom_model_data}, one numeral per settable rank and stage.
 *
 * <p>The kinds are names, not callbacks, so a registration line never references a client datagen class.
 *
 * @author Alex
 * @version 1.0.0
 * @see ItemBuilder#model
 * @since 1.0.0
 */

public enum ItemModel { FLAT, BLOCK, FLUID, LEAF_STAGES, NUMERALS }
