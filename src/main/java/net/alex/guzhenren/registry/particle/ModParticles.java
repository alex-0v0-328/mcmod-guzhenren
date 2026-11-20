package net.alex.guzhenren.registry.particle;

import net.alex.guzhenren.Guzhenren;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.core.registries.Registries;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * 本模组全部自定义粒子类型（{@link SimpleParticleType}）注册表，一粒子对应一个常量（精灵图定义由
 * {@code ModParticleDescriptionProvider} 生成，{@code textures/particle/} 贴图为手绘终稿，客户端渲染见
 * {@link net.alex.guzhenren.client.particle.RingParticle}）。
 * <ul>
 *   <li>{@link #SHOCKWAVE_RING}：冲刺尾迹环，在冲刺路径各落点由小到大原地绽开（冲刺者第一人称视角不可见）。</li>
 *   <li>{@link #SONIC_BOOM_RING}：音爆环，进入二段飞行[Second-Stage Flight]或高速撞面时生成，绽开至约 4 格并在停留期淡
 *       出。</li>
 *   <li>{@link #BARRIER_RING}：音障环，高速飞行期间在前方不断成形，淡出阶段扩大至最大帧尺寸的四倍。</li>
 *   <li>{@link #AREA_DAMAGE_RING}：范围伤害环，每次触发范围伤害生成一枚，自锥尖行进至锥底后如音障环般扩散淡出。</li>
 * </ul>
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
    public static final DeferredHolder<ParticleType<?>, SimpleParticleType> AREA_DAMAGE_RING = PARTICLE_TYPES.register(
            "area_damage_ring", () -> new SimpleParticleType(false));

    public static void register(IEventBus modEventBus) { PARTICLE_TYPES.register(modEventBus); }
}
