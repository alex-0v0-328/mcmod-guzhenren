package net.alex.guzhenren.client.icon;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.effect.MobEffectInstance;

/**
 * Draws an effect with the icon of the item that grants it, so the two always read as the same thing.
 *
 * <p>Implements {@link net.alex.guzhenren.client.icon.EffectIconLayout}, carrying only the item's registration id
 * and the texture location built from it once. The texture path is always {@code item/<id>}, ignoring the
 * amplifier -- a non-graded effect has one face.
 *
 * <p>⚠ It takes the item's registration id, not a mob_effect texture. An effect wearing its Gu's face
 * needs no second drawing, and a missing one here would be a checkerboard rather than a fallback.
 *
 * @author Alex
 * @version 1.0.0
 * @see net.alex.guzhenren.client.icon.EffectIconLayout
 * @see net.alex.guzhenren.client.icon.GradedEffectIcon
 * @since 1.0.0
 */

public final class ItemEffectIcon implements EffectIconLayout {

    private final String texture;
    private final ResourceLocation location;

    public ItemEffectIcon(String item) {
        this.texture = "item/" + item;
        this.location = EffectIconLayout.getLocationOf(texture);
    }

    @Override
    public String getTextureFor(MobEffectInstance instance) { return texture; }

    @Override
    public ResourceLocation getLocationFor(MobEffectInstance instance) { return location; }
}
