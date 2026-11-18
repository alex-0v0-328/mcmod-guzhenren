package net.alex.guzhenren.gameplay.flight;

import org.jetbrains.annotations.NotNull;

/**
 * 空中飞行能力授权等级枚举：按无飞行（{@link #NONE}）、普通创造式飞行（{@link #FLIGHT}）与二段飞行[Second-Stage Flight]
 * （{@link #SECOND_STAGE}，含 {@link SecondStageService} 高速巡航）三级递进；多来源并存时由 {@link #getStrongest} 取最
 * 高档，互不叠加。
 */

public enum FlightGrant {

    NONE,
    FLIGHT,
    SECOND_STAGE;

    public boolean canFly() { return this != NONE; }

    public boolean canFlySecondStage() { return this == SECOND_STAGE; }

    public static @NotNull FlightGrant getStrongest(@NotNull FlightGrant first, @NotNull FlightGrant second) {
        return first.ordinal() >= second.ordinal() ? first : second;
    }
}
