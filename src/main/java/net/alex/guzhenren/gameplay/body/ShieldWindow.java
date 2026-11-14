package net.alex.guzhenren.gameplay.body;

/**
 * 护盾最近遇到的无敌帧：最大伤害与关闭 tick；临时附件 {@code shield_window}，不存不同步，仅 {@code ShieldService} 写。
 * 原版 {@code lastHurt} 不能替代：全额吃下的伤害被取消会遗留它，击穿护盾的只留放过部分；本记录未见的帧读
 * {@code lastHurt}。
 *
 * @author Alex
 * @version 1.0.0
 * @see ShieldService
 * @since 1.0.0
 */

public record ShieldWindow(float lastHit, long endTick) {

    public static final ShieldWindow CLOSED = new ShieldWindow(0.0F, BodyData.UNTRACKED);

    public boolean isOpen(long now) { return now < endTick; }
}
