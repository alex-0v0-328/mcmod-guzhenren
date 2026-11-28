package net.alex.guzhenren.client;

import net.alex.guzhenren.gameplay.aperture.Aperture;
import net.alex.guzhenren.gameplay.aperture.Rank;
import net.alex.guzhenren.gameplay.aperture.Stage;
import org.jetbrains.annotations.Nullable;

/**
 * 真元海配色表：B 面板空窍插图与 HUD 真元主珠共用。
 * <ul>
 *   <li>共 20 种境界配色，色号按 {@code (转 - 1) × 4 + 阶} 计算（初 0、中 1、高 2、巅峰 3）；每种配色四阶由暗到亮为
 *       深、中、浅、浪尖。未开窍或六转及以上没有配色（{@link #getColors} 返回 {@code null}）。</li>
 * </ul>
 */

public final class ApertureSea {

    public static final int DEEP = 0;
    public static final int MID = 1;
    public static final int LIGHT = 2;
    public static final int CREST = 3;
    private static final int STAGES = 4;
    private static final int[][] COLORS = {
            { 0xFF123D2C, 0xFF1F7A4A, 0xFF3FB86A, 0xFFA8F0B0 },
            { 0xFF1C3330, 0xFF3A6650, 0xFF5F9470, 0xFFB6D8A8 },
            { 0xFF0E2A26, 0xFF1D5238, 0xFF2F7D4C, 0xFF86C48A },
            { 0xFF0A1A1C, 0xFF163A30, 0xFF245A42, 0xFF5F9A74 },
            { 0xFF5A2234, 0xFFB3474E, 0xFFE8806E, 0xFFFFD0B0 },
            { 0xFF4A1028, 0xFFA82434, 0xFFE04A3E, 0xFFFFB08A },
            { 0xFF36081E, 0xFF7E1828, 0xFFB62E2E, 0xFFF07E62 },
            { 0xFF22061A, 0xFF541222, 0xFF86202A, 0xFFC45848 },
            { 0xFF4A4C5E, 0xFF80869A, 0xFFAAB0BE, 0xFFE2E6EA },
            { 0xFF4E5470, 0xFF8A94AC, 0xFFBCC4D2, 0xFFF2F4F6 },
            { 0xFF5A6680, 0xFF9CACC2, 0xFFD4DEE8, 0xFFFFFFFF },
            { 0xFF6C7A92, 0xFFB8C6D6, 0xFFEAF0F6, 0xFFFFFFFF },
            { 0xFF6A4E2C, 0xFFB0924E, 0xFFDCC27C, 0xFFFFF2C4 },
            { 0xFF7A4A14, 0xFFD0942A, 0xFFF4CC4A, 0xFFFFF6B0 },
            { 0xFF6E3410, 0xFFC07A1A, 0xFFECAC2E, 0xFFFFE48A },
            { 0xFF7A2C0C, 0xFFD2781A, 0xFFFFBC22, 0xFFFFF08A },
            { 0xFF4C3A6E, 0xFF8A70B0, 0xFFBCA2DC, 0xFFF0E2FF },
            { 0xFF5A1E5E, 0xFFA2449E, 0xFFD070C8, 0xFFFFC4EC },
            { 0xFF22124E, 0xFF4A2A8A, 0xFF7446B8, 0xFFC49AF0 },
            { 0xFF2A1C74, 0xFF5A34C8, 0xFF9662FF, 0xFFE4CCFF },
    };

    private ApertureSea() {}

    public static int @Nullable [] getColors(Aperture aperture) {
        Rank rank = aperture.rank();
        if (rank == Rank.NONE || rank.compareTo(Rank.FIVE) > 0 || aperture.stage() == Stage.NONE) return null;

        return COLORS[(rank.ordinal() - Rank.ONE.ordinal()) * STAGES + aperture.stage().ordinal()
                - Stage.INITIAL.ordinal()];
    }
}
