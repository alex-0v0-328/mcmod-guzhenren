package net.alex.guzhenren.gameplay.place;

import org.jetbrains.annotations.NotNull;

/**
 * 地名标题[Place Title]的内容：上方小字类别（如「天地秘境」）、下方大字地名与地名颜色（RGB）；两段都是语言键，由客户端
 * 按本机语言翻译。显示与冷却见 {@link PlaceTitles}。
 */

public record PlaceTitle(@NotNull String kindKey, @NotNull String nameKey, int color) {}
