package net.alex.guzhenren.mixin;

import net.alex.guzhenren.gameplay.body.LastHurtView;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/**
 * {@code LivingEntity} 上 {@link LastHurtView} 的实现：{@code lastHurt} 字段访问器。
 * ⚠ 不用事件或 API：{@code lastHurt} 受保护，无 NeoForge 事件暴露（{@code DamageContainer} 只带减伤）；访问转换器也行，
 * 但须启用 {@code build.gradle} 里注释掉的 {@code accessTransformers} 行，访问器使 {@link LastHurtView} 为唯一接缝。
 *
 * @author Alex
 * @version 1.0.0
 * @since 1.0.0
 */

@Mixin(LivingEntity.class)
public interface LivingEntityAccessor extends LastHurtView {

    @Override
    @Accessor("lastHurt")
    float guzhenren$getLastHurt();
}
