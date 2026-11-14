package net.alex.guzhenren.gameplay.aperture;

import com.mojang.serialization.Codec;
import net.alex.guzhenren.core.NamedEnum;
import net.minecraft.util.StringRepresentable;

/**
 * 空窍[Aperture]状态：封闭词汇枚举，只推导不存储。
 *
 * <ul>
 *   <li>{@code NORMAL} 活窍；{@code DEAD}（死窍）含僵尸、临时僵尸化、石窍，不自然回复、温养[Nourish]或冲刷窍壁。</li>
 *   <li>⚠ 推导在 {@code ApertureService#getStatus} 而非此处：本包枚举只作词汇，门槛都判 {@code status == NORMAL}。</li>
 * </ul>
 *
 * @author Alex
 * @version 1.0.0
 * @since 1.0.0
 */

public enum ApertureStatus implements NamedEnum {

    NORMAL, DEAD;
    public static final Codec<ApertureStatus> CODEC = StringRepresentable.fromEnum(ApertureStatus::values);

    @Override
    public String getTranslationPrefix() { return "guzhenren.enum.aperture.status."; }
}
