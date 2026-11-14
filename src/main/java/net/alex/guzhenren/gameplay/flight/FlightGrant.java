package net.alex.guzhenren.gameplay.flight;

import org.jetbrains.annotations.NotNull;

/**
 * 来源允许的空中能力，三级递进。{@code FLIGHT}：创造式飞行，原版疾跑可加速；{@code SECOND_STAGE}：二段飞
 * 行[Second-Stage Flight]，再加 {@link SecondStageService} 管的更快一档；{@link #getStrongest} 取较高级，多来源不叠过最
 * 高级。
 *
 * @author Alex
 * @version 1.0.0
 * @see FlightContributor
 * @since 1.0.0
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
