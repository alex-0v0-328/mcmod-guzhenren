package net.alex.guzhenren.datagen.particle;

import net.alex.guzhenren.Guzhenren;
import net.alex.guzhenren.registry.particle.ModParticles;
import net.minecraft.data.PackOutput;
import net.neoforged.neoforge.common.data.ExistingFileHelper;
import net.neoforged.neoforge.common.data.ParticleDescriptionProvider;

/**
 * Generates {@code assets/guzhenren/particles/*.json}: the sprite list per particle type, in
 * playback order. Both trails list the rings smallest-to-largest (every ring blooms where it
 * is born): the dash trail plants them along the path, the punch trail plants
 * them along the punch ray from the strike point. The type split is semantic: the dash trail
 * hides from the dasher's own first-person camera, the punch cone does not.
 *
 * @author Alex
 * @version 1.0.0
 * @see net.alex.guzhenren.client.particle.RingParticle
 * @since 1.0.0
 */

public final class ModParticleDescriptionProvider extends ParticleDescriptionProvider {

    public ModParticleDescriptionProvider(PackOutput output, ExistingFileHelper fileHelper) {
        super(output, fileHelper);
    }

    @Override
    protected void addDescriptions() {
        this.spriteSet(ModParticles.SHOCKWAVE_RING.get(),
                Guzhenren.id("ring_3x3"), Guzhenren.id("ring_8x8"), Guzhenren.id("ring_13x13"),
                Guzhenren.id("ring_18x18"), Guzhenren.id("ring_23x23"));
        this.spriteSet(ModParticles.IMPACT_RING.get(),
                Guzhenren.id("ring_3x3"), Guzhenren.id("ring_8x8"), Guzhenren.id("ring_13x13"),
                Guzhenren.id("ring_18x18"), Guzhenren.id("ring_23x23"));
    }
}
