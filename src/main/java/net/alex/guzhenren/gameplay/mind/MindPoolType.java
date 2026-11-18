package net.alex.guzhenren.gameplay.mind;

import com.mojang.serialization.Codec;
import net.alex.guzhenren.core.NamedEnum;
import net.minecraft.util.StringRepresentable;

/**
 * 脑海三池种类枚举：定义念（{@link #THOUGHTS}）、意（{@link #WILLS}）、情（{@link #EMOTIONS}）的默认容量与是否允许超限
 * 爆裂。
 * <ul>
 *   <li>各池默认容量（{@code 50000 / 12 / 8}）与超限致死比例（{@link #BURST_NUMERATOR} / {@link #BURST_DENOMINATOR}）唯
 *       一定义在此。</li>
 *   <li>⚠ 只有 {@link #isBurstable} 为 {@code true} 的池允许写入超出 {@code max} 的缓冲值并在超限时触发脑海炸裂致死，调
 *       用方一律查询该方法而非硬编码枚举常量。</li>
 * </ul>
 */

public enum MindPoolType implements NamedEnum {

    THOUGHTS(50_000L, true),
    WILLS(12L, false),
    EMOTIONS(8L, false);

    public static final Codec<MindPoolType> CODEC = StringRepresentable.fromEnum(MindPoolType::values);
    public static final long BURST_NUMERATOR = 6L;
    public static final long BURST_DENOMINATOR = 5L;
    private final long defaultCapacity;
    private final boolean burstable;

    MindPoolType(long defaultCapacity, boolean burstable) {
        this.defaultCapacity = defaultCapacity;
        this.burstable = burstable;
    }

    public long getDefaultCapacity() { return defaultCapacity; }

    public boolean isBurstable() { return burstable; }

    @Override
    public String getTranslationPrefix() { return "guzhenren.enum.wisdom.type."; }
}
