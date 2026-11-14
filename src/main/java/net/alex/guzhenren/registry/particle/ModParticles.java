package net.alex.guzhenren.registry.particle;

import net.alex.guzhenren.Guzhenren;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.core.registries.Registries;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * 全部粒子类型，一粒子一常量。
 *
 * <ul>
 *   <li>精灵图由 {@code ModParticleDescriptionProvider} 生成，贴图 {@code textures/particle/} 为 Alex 手绘终稿。</li>
 *   <li>{@link #SHOCKWAVE_RING}：冲刺尾迹，环由小到大，在冲刺路径各落点原地绽开；冲刺者第一人称不可见。</li>
 *   <li>{@link #SONIC_BOOM_RING}：音爆环，进入二段飞行[Second-Stage Flight]时出现，同款环绽至约 4 格，停留期淡出。</li>
 *   <li>{@link #BARRIER_RING}：音障环，高速飞行时在前端成形，淡出时扩至最大帧四倍。</li>
 *   <li>{@link #AREA_IMPACT_RING}：范围冲击环，每次范围冲击一个，自锥尖行至锥末如音障环扩散，停下淡出。</li>
 * </ul>
 *
 * @author Alex
 * @version 1.0.0
 * @see net.alex.guzhenren.client.particle.RingParticle
 * @since 1.0.0
 */

public final class ModParticles {

    private ModParticles() {}

    public static final DeferredRegister<ParticleType<?>> PARTICLE_TYPES =
            DeferredRegister.create(Registries.PARTICLE_TYPE, Guzhenren.MOD_ID);
    public static final DeferredHolder<ParticleType<?>, SimpleParticleType> SHOCKWAVE_RING = PARTICLE_TYPES.register(
            "shockwave_ring", () -> new SimpleParticleType(false));
    public static final DeferredHolder<ParticleType<?>, SimpleParticleType> SONIC_BOOM_RING = PARTICLE_TYPES.register(
            "sonic_boom_ring", () -> new SimpleParticleType(false));
    public static final DeferredHolder<ParticleType<?>, SimpleParticleType> BARRIER_RING = PARTICLE_TYPES.register(
            "barrier_ring", () -> new SimpleParticleType(false));
    public static final DeferredHolder<ParticleType<?>, SimpleParticleType> AREA_IMPACT_RING = PARTICLE_TYPES.register(
            "area_impact_ring", () -> new SimpleParticleType(false));

    public static void register(IEventBus modEventBus) { PARTICLE_TYPES.register(modEventBus); }
}
