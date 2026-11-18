package net.alex.guzhenren.gameplay.flight;

/**
 * 飞行蛊药水效果授予飞行权限的接口：按效果放大器等级返回普通飞行或含二段飞行[Second-Stage Flight]的
 * {@link FlightGrant}；{@link FlightService} 每 tick 遍历玩家活动效果取最高档（无需硬编码具体效果类），授权随药水效果共
 * 存亡且不写入存档。
 */

public interface FlightContributor {

    FlightGrant getFlightGrant(int amplifier);
}
