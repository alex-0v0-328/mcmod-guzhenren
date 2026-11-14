package net.alex.guzhenren.datagen.particle;

import java.util.List;
import net.alex.guzhenren.Guzhenren;
import net.alex.guzhenren.registry.particle.ModParticles;
import net.minecraft.data.PackOutput;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.common.data.ExistingFileHelper;
import net.neoforged.neoforge.common.data.ParticleDescriptionProvider;

/**
 * {@code assets/guzhenren/particles/*.json}：各粒子类型按播放顺序的 sprite 列表。
 *
 * <ul>
 *   <li>各环类型列同一组由小到大的环（{@link #RINGS}），每环在出生处绽开。</li>
 *   <li>冲刺拖尾沿路径种环；二段飞行[Second-Stage Flight]每次进入处种 1 个音爆，高速时前缘种音障环；范围冲击沿锥体送 1
 *       环。</li>
 *   <li>按语义分型：冲刺拖尾对冲刺者第一人称隐藏；音爆长到自身尺寸后淡出；音障环边扩边淡；范围冲击环至锥体末端才淡
 *       出。</li>
 * </ul>
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
        this.spriteSet(ModParticles.SONIC_BOOM_RING.get(), RINGS);
        this.spriteSet(ModParticles.BARRIER_RING.get(), RINGS);
        this.spriteSet(ModParticles.AREA_IMPACT_RING.get(), RINGS);
    }
}
