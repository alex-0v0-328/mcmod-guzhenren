package net.alex.guzhenren.gameplay.attribute;

import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import org.jetbrains.annotations.NotNull;

/**
 * 本模组操作原版实体属性的唯一入口：在固定 {@link ResourceLocation} 下幂等挂载或移除临时 {@link AttributeModifier}。
 * <ul>
 *   <li>{@code ADD_VALUE}（三参数 {@link #swap}）：供
 *       {@link net.alex.guzhenren.gameplay.path.strength.AttackDamageService}、{@link MaxHealthService} 与
 *       {@link KnockbackResistanceService} 使用。</li>
 *   <li>{@code ADD_MULTIPLIED_BASE}：供 Epic Fight 耐力上限（{@code compat.EpicFightIntegration}）使用。</li>
 *   <li>{@code ADD_MULTIPLIED_TOTAL}：供兽力[Beast Strength]疾跑速度
 *       （{@link net.alex.guzhenren.gameplay.path.strength.SprintSpeedService}）与坠击重力缩放
 *       （{@link #setGravityFactor}）使用。</li>
 *   <li>⚠ 必须使用临时修饰符（{@code addTransientModifier}）：永久修饰符会序列化进实体属性 NBT，重登后与代码重新挂载的
 *       修饰符叠加或冲突。</li>
 *   <li>⚠ 当目标属性已持有同 ID、同数值、同运算方式的修饰符（{@link #hasModifier}），或当前无修饰符且新值为 {@code 0}
 *       时，{@link #swap} 直接返回不触碰属性实例，避免每次心跳或写入刷新时重复触发原版属性同步包。</li>
 * </ul>
 */

public final class TransientModifiers {

    private TransientModifiers() {}

    /** 对实体该属性调 {@link #swap(AttributeInstance, ResourceLocation, double)}；无此属性不做事。 */
    public static void swap(@NotNull LivingEntity entity, @NotNull Holder<Attribute> attribute,
                            @NotNull ResourceLocation id, double amount) {
        AttributeInstance instance = entity.getAttribute(attribute);
        if (instance != null) swap(instance, id, amount);
    }

    public static void swap(@NotNull AttributeInstance instance, @NotNull ResourceLocation id, double amount) {
        swap(instance, id, amount, AttributeModifier.Operation.ADD_VALUE);
    }

    public static void swap(@NotNull AttributeInstance instance, @NotNull ResourceLocation id, double amount,
            @NotNull AttributeModifier.Operation operation) {
        if (instance.getModifier(id) == null ? amount == 0.0D : hasModifier(instance, id, amount, operation)) return;

        instance.removeModifier(id);
        if (amount != 0.0D) instance.addTransientModifier(new AttributeModifier(id, amount, operation));
    }

    /** 在固定 {@code id} 下将实体的原版同步 {@code GRAVITY} 属性乘以 {@code factor}（{@code 1} 时移除修饰符）。 */
    public static void setGravityFactor(@NotNull LivingEntity entity, @NotNull ResourceLocation id, double factor) {
        AttributeInstance gravity = entity.getAttribute(Attributes.GRAVITY);
        if (gravity != null) swap(gravity, id, factor - 1.0D, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL);
    }

    public static boolean hasModifier(@NotNull AttributeInstance instance, @NotNull ResourceLocation id, double amount,
                                      @NotNull AttributeModifier.Operation operation) {
        AttributeModifier held = instance.getModifier(id);
        return held != null && held.amount() == amount && held.operation() == operation;
    }
}
