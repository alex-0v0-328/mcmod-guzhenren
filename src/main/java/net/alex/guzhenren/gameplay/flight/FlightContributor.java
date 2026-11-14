package net.alex.guzhenren.gameplay.flight;

/**
 * 飞行蛊效果授予飞行的唯一接口：按效果等级返回仅飞行或含二段飞行[Second-Stage Flight]。{@link FlightService} 每 tick
 * 遍历 {@code getActiveEffects()} 取最强，不点名效果，新飞行蛊只需实现此接口；授权与效果同存亡，不进存档。
 *
 * @author Alex
 * @version 1.0.0
 * @see FlightService
 * @since 1.0.0
 */

public interface FlightContributor {

    FlightGrant getFlightGrant(int amplifier);
}
