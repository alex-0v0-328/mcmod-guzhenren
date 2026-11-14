package net.alex.guzhenren.entity.beast;

import java.util.EnumMap;
import java.util.Map;
import java.util.function.Function;
import net.alex.guzhenren.gameplay.path.strength.BeastStrengthFamily;
import net.minecraft.world.phys.Vec3;
import software.bernie.geckolib.animation.RawAnimation;

/**
 * 兽力虚影渲染器读的接口：借哪个兽种模型、淡入淡出程度、鼻尖下俯（MC 符号，向下为正）、是否冲锋。
 * <ul>
 *   <li>身旁虚影与兽力冲击均实现，仅冲击会俯仰、冲锋；仅身旁虚影有出现与消失：{@link #getGlow} 为发光层相对平时倍数，
 *       {@link #getScale} 为模型大小，{@link #getDrift} 为模型相对实体、按自身朝向右上前的偏移格数。</li>
 *   <li>动画命名不一：野猪无前缀（{@code idle}、{@code appear}），熊、虎、水牛带 {@code animation.}，由
 *       {@link #getAppear}、{@link #getLoop} 屏蔽；龟带前缀而无出场动画，只待机；马无动画文件，原版马模型自摆姿势
 *       （{@code HorsePhantomModel}）。</li>
 * </ul>
 *
 * @author Alex
 * @version 1.0.0
 * @see BeastPhantomEntity
 * @see net.alex.guzhenren.entity.projectile.BeastChargeEntity
 * @since 1.0.0
 */

public interface BeastPhantomVisual {

    BeastStrengthFamily getFamily();

    float getFade(float partialTick);

    default float getGlow(float partialTick) { return getFade(partialTick); }

    default float getScale(float partialTick) { return 1.0F; }

    default Vec3 getDrift(float partialTick) { return Vec3.ZERO; }

    default float getPitch(float partialTick) { return 0.0F; }

    default boolean isCharging() { return false; }

    static Map<BeastStrengthFamily, RawAnimation> byFamily(Function<BeastStrengthFamily, RawAnimation> animation) {
        Map<BeastStrengthFamily, RawAnimation> animations = new EnumMap<>(BeastStrengthFamily.class);
        for (BeastStrengthFamily family : BeastStrengthFamily.values()) animations.put(family, animation.apply(family));
        return animations;
    }

    static RawAnimation getAppear(BeastStrengthFamily family, String loop) {
        if (family == BeastStrengthFamily.TURTLE) return getLoop(family, loop);
        return RawAnimation.begin().thenPlay(getName(family, "appear")).thenLoop(getName(family, loop));
    }

    static RawAnimation getLoop(BeastStrengthFamily family, String loop) {
        return RawAnimation.begin().thenLoop(getName(family, loop));
    }

    private static String getName(BeastStrengthFamily family, String name) {
        return isBareNamed(family) ? name : "animation." + name;
    }

    private static boolean isBareNamed(BeastStrengthFamily family) {
        return family == BeastStrengthFamily.BOAR;
    }
}
