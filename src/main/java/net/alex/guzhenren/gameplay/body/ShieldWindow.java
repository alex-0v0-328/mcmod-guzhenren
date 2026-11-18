package net.alex.guzhenren.gameplay.body;

/**
 * 护盾受击无敌帧窗口记录（服务端临时内存附件 {@code shield_window}，不落盘、不同步，仅由 {@link ShieldService} 写入）：
 * 记录当前无敌帧段内已承受的最大伤害与窗口结束 tick；因被护盾全额挡下并取消的事件不会更新原版 {@code lastHurt}，而击穿
 * 护盾的事件只会在 {@code lastHurt} 留下放行余量，故需单独跟踪窗口峰值（窗口未开启时回退读 {@code lastHurt}）。
 */

public record ShieldWindow(float lastHit, long endTick) {

    public static final ShieldWindow CLOSED = new ShieldWindow(0.0F, BodyData.UNTRACKED);

    public boolean isOpen(long now) { return now < endTick; }
}
