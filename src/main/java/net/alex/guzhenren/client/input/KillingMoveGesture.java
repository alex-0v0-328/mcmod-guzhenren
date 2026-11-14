package net.alex.guzhenren.client.input;

import java.util.List;
import java.util.function.Consumer;
import net.alex.guzhenren.item.gu.killingmove.KillingMoveService;
import org.jetbrains.annotations.NotNull;

/**
 * 月刃蛊一次按住使用键的发送内容；不碰游戏状态，纯 JUnit 可测。
 * <ul>
 *   <li>轮盘回调进、payload 招式 id 出；轻点（弹出前松开）发普通月刃 {@link KillingMoveService#PLAIN}；
 *       无可用杀招[Killing Move]（{@link #empty}）不发，虽轮盘同样经 {@link #releaseUnopened} 报松开；选中发
 *       {@link #list} 在该扇区放的 id。</li>
 * </ul>
 *
 * @author Alex
 * @version 1.0.0
 * @see KillingMoveInput
 * @since 1.0.0
 */

public final class KillingMoveGesture {

    private final Consumer<String> send;
    private List<String> listed = List.of();
    private boolean emptied;

    public KillingMoveGesture(@NotNull Consumer<String> send) {
        this.send = send;
    }

    public void list(@NotNull List<String> ids) { listed = List.copyOf(ids); }

    public void empty() { emptied = true; }

    public void pick(int index) { send.accept(listed.get(index)); }

    public void releaseUnopened() {
        if (!emptied) send.accept(KillingMoveService.PLAIN);
    }
}
