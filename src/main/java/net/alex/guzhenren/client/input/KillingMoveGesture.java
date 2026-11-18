package net.alex.guzhenren.client.input;

import java.util.List;
import java.util.function.Consumer;
import net.alex.guzhenren.item.gu.killingmove.KillingMoveService;
import org.jetbrains.annotations.NotNull;

/**
 * 月刃蛊一次按住使用键时的轮盘结果映射器：不依赖游戏状态，可直接由纯 JUnit 单元测试覆盖。
 * <ul>
 *   <li>负责把轮盘菜单回调转化为待发送的杀招[Killing Move]载荷 ID：轻点（弹出前松开）发送普通月刃
 *       {@link KillingMoveService#PLAIN}；无可用杀招（{@link #empty}）时不发包（即便轮盘同样会经
 *       {@link #releaseUnopened} 通知松开）；选中扇区则发送 {@link #list} 在该位置绑定的招式 ID。</li>
 * </ul>
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
