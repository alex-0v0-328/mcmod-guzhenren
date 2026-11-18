package net.alex.guzhenren.datagen.particle;

import java.util.List;
import net.alex.guzhenren.Guzhenren;
import net.alex.guzhenren.registry.particle.ModParticles;
import net.minecraft.data.PackOutput;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.common.data.ExistingFileHelper;
import net.neoforged.neoforge.common.data.ParticleDescriptionProvider;

/**
 * 粒子贴图序列描述文件（{@code assets/guzhenren/particles/*.json}）的数据生成器。
 * <ul>
 *   <li>四类环形粒子（冲刺冲击波环、音爆环、音障环、范围冲击环）共用同一组由小到大排列的 5 帧环形贴图序列
 *       （{@link #RINGS}）。</li>
 *   <li>四者按客户端行为语义拆分为独立粒子类型（{@link net.alex.guzhenren.client.particle.RingParticle}）：冲刺冲击波环
 *       对冲刺者本人第一人称隐藏，音爆环扩张至目标尺寸后淡出，音障环边扩张边淡出，范围冲击环则沿锥体推进至末端后淡
 *       出。</li>
 * </ul>
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
