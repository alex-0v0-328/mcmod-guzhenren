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
 * 本 mod 写属性的唯一方式：固定 id 下的临时修饰。
 * <ul>
 *   <li>{@code ADD_VALUE}（三参数 {@link #swap}）：{@code AttackDamageService}、{@link MaxHealthService}、
 *       {@link KnockbackResistanceService}。</li>
 *   <li>{@code ADD_MULTIPLIED_BASE}：Epic Fight 耐力上限（{@code compat.EpicFightIntegration}）。</li>
 *   <li>{@code ADD_MULTIPLIED_TOTAL}：
 *       兽力[Beast Strength]疾跑速度（{@link net.alex.guzhenren.gameplay.path.strength.SprintSpeedService}）。</li>
 *   <li>⚠ 有意用临时修饰：永久修饰存进属性 NBT，下次登录叠两份。</li>
 *   <li>⚠ 已 {@link #hasModifier} 该修饰或无修饰且值为 0 时 {@link #swap} 不动：每写、每心跳都刷新，重发即抖同步。</li>
 * </ul>
 *
 * @author Alex
 * @version 1.0.0
 * @see net.alex.guzhenren.gameplay.path.strength.AttackDamageService
 * @see MaxHealthService
 * @since 1.0.0
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

    /** 经原版同步的 {@code GRAVITY} 在 {@code id} 下把重力乘 {@code factor}（坠击）；为 1 则摘修饰。 */
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
