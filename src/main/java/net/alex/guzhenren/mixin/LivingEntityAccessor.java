package net.alex.guzhenren.mixin;

import net.alex.guzhenren.gameplay.body.LastHurtView;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/**
 * {@link LivingEntity} 上 {@link LastHurtView} 的受保护字段 {@code lastHurt} 访问器，作为读取最近一次受伤基准值的唯一接
 * 缝。
 * <ul>
 *   <li>⚠ 不使用事件或 API：{@code lastHurt} 为受保护字段且 NeoForge 事件未暴露该原始字段（{@code DamageContainer} 仅包
 *       含减伤信息）；虽可改用访问转换器，但需开启 {@code build.gradle} 中已注释的配置，使用访问器更有利于把边界收口在
 *       {@link LastHurtView}。</li>
 * </ul>
 */

@Mixin(LivingEntity.class)
public interface LivingEntityAccessor extends LastHurtView {

    @Override
    @Accessor("lastHurt")
    float guzhenren$getLastHurt();
}
