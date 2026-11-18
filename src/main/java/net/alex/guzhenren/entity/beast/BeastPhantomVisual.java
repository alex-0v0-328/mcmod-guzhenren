package net.alex.guzhenren.entity.beast;

import java.util.EnumMap;
import java.util.Map;
import java.util.function.Function;
import net.alex.guzhenren.gameplay.path.strength.BeastStrengthFamily;
import net.minecraft.world.phys.Vec3;
import software.bernie.geckolib.animation.RawAnimation;

/**
 * 兽力虚影渲染器读取的视觉契约接口：提供兽种类型、淡入淡出系数、俯仰角（Minecraft 符号约定，向下为正）以及是否处于冲锋
 * 状态。
 * <ul>
 *   <li>身旁虚影（{@link BeastPhantomEntity}）与兽力冲击
 *       （{@link net.alex.guzhenren.entity.projectile.BeastChargeEntity}）均实现本接口：仅兽力冲击会俯仰与冲锋；仅身旁
 *       虚影具有出现与收拢形变，其中 {@link #getGlow} 为发光层相对常驻亮度的倍率，{@link #getScale} 为模型缩放倍
 *       率，{@link #getDrift} 为模型相对实体原点沿自身右、上、前方向的偏移格数。</li>
 *   <li>各兽种模型动画命名不一致：野猪无前缀（{@code idle}、{@code appear}），熊、虎、水牛带 {@code animation.} 前缀，
 *       统一由 {@link #getAppear} 与 {@link #getLoop} 屏蔽；龟带前缀但无出场动画，直接循环待机；马无 GeckoLib 动画文
 *       件，由原版马模型 {@code HorsePhantomModel} 自行摆出姿态。</li>
 * </ul>
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
