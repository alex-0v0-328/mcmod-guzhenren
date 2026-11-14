package net.alex.guzhenren.gameplay.mind;

import com.mojang.serialization.Codec;
import net.alex.guzhenren.core.NamedEnum;
import net.minecraft.util.StringRepresentable;

/**
 * 脑海池的种类：念、意、情，对应 {@code MindData} 三池；闭合枚举。
 *
 * <ul>
 *   <li>容量 {@code 50000 / 12 / 8} 只在此声明，超限比例只在 {@code BURST_NUMERATOR/DENOMINATOR}。</li>
 *   <li>⚠ 只有部分池能超上限，也只有它们能致死；{@code isBurstable()} 是唯一声明，调用处不得再判常量。</li>
 * </ul>
 *
 * @author Alex
 * @version 1.0.0
 * @since 1.0.0
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
