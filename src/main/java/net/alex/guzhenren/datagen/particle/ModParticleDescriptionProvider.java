package net.alex.guzhenren.datagen.particle;

import java.util.List;
import net.alex.guzhenren.Guzhenren;
import net.alex.guzhenren.registry.particle.ModParticles;
import net.minecraft.data.PackOutput;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.common.data.ExistingFileHelper;
import net.neoforged.neoforge.common.data.ParticleDescriptionProvider;

/**
 * Generates {@code assets/guzhenren/particles/*.json}: the sprite list per particle type, in
 * playback order. Every ring type lists the same rings smallest-to-largest ({@link #RINGS}; every ring
 * blooms where it is born): the dash trail plants them along the path, the punch trail plants
 * them along the punch ray from the strike point, second-stage flight [二段飞行] plants one sonic boom where each
 * entry starts and sound barrier rings at its leading face while it stays fast. The type split is semantic: the
 * dash trail hides from the dasher's own first-person camera, the punch cone does not, the sonic boom grows to its
 * own size and fades, and a sound barrier ring widens further as it fades.
 *
 * @author Alex
 * @version 1.0.0
 * @see net.alex.guzhenren.client.particle.RingParticle
 * @since 1.0.0
 */

public final class ModParticleDescriptionProvider extends ParticleDescriptionProvider {

    private static final List<ResourceLocation> RINGS = List.of(
            Guzhenren.id("ring_3x3"), Guzhenren.id("ring_8x8"), Guzhenren.id("ring_13x13"),
            Guzhenren.id("ring_18x18"), Guzhenren.id("ring_23x23"));

    public ModParticleDescriptionProvider(PackOutput output, ExistingFileHelper fileHelper) {
        super(output, fileHelper);
    }

    @Override
    protected void addDescriptions() {
        this.spriteSet(ModParticles.SHOCKWAVE_RING.get(), RINGS);
        this.spriteSet(ModParticles.IMPACT_RING.get(), RINGS);
        this.spriteSet(ModParticles.SONIC_BOOM_RING.get(), RINGS);
        this.spriteSet(ModParticles.BARRIER_RING.get(), RINGS);
    }
}
