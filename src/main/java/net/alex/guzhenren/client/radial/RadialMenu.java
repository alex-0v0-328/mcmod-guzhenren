package net.alex.guzhenren.client.radial;

import com.mojang.blaze3d.platform.Window;
import java.util.List;
import net.alex.guzhenren.Guzhenren;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.InputEvent;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * 长按呼出的扇形选择器控制器。
 * <ul>
 *   <li>由 {@link Owner} 决定何时处于按住、展示哪些条目及选中后的回调；现有调用方包括功倍蛊[Amplify Effect Gu]与
 *       月刃蛊[Moon Blade Gu]（选择其杀招[Killing Move]），每个 {@link Entry} 对应一个扇区及其图标、中心名称和选中动
 *       作。</li>
 *   <li>按住期间在屏幕中心升起扇形菜单，移动光标指向扇区并松手即触发选中；同一时间仅维持一个活跃长按（{@link #hold}），
 *       须按住满 {@link #OPEN_DELAY_TICKS} tick 才正式弹出（轻点不显示），条目列表在弹出瞬间读取一次并在松开前保持不
 *       变。</li>
 *   <li>松开按键时若落在启用扇区则调用 {@link Owner#pick}，落在内圈死区或禁用扇区则无事发生；若完全没有条目则调用一
 *       次 {@link Owner#empty} 并等待松开；弹出前松开或在 {@code empty} 后松开均调用 {@link Owner#releaseUnopened}（默
 *       认空操作）；打开界面或登出时直接丢弃状态而不调用任何回调。</li>
 *   <li>⚠ {@link Owner#isHeld} 因其他原因变为 false（例如物品被切走离手）同样会立即结束长按并照常回调 {@code pick}
 *       或 {@code releaseUnopened}，因此会发包的持有者须自行再次确认物品仍在手中。</li>
 *   <li>⚠ 菜单弹出期间相机视角锁定而玩家移动照常：{@code MouseHandlerMixin} 把本应转动镜头的鼠标位移（按屏幕像素）
 *       改交给 {@link #steer}，推动一枚被限制在外圈半径内的不可见光标，仅通过高亮对应扇区反馈指向。</li>
 *   <li>长按期间屏蔽交互键向世界的穿透及快捷栏滚轮滚动，使物品留在手中且同一点击不会意外攻击或放置方块。</li>
 *   <li>动画按墙钟时间由 {@link RadialMotion} 推进并由 {@link RadialDrawing} 绘制；松开后立即结束长按（恢复镜头并派发
 *       选中结果），仅保留用于绘制离场动画的 {@code Exit} 状态。</li>
 * </ul>
 */

@EventBusSubscriber(modid = Guzhenren.MOD_ID, value = Dist.CLIENT)
public final class RadialMenu {

    public record Entry(@NotNull ItemStack icon, @NotNull Component label, boolean enabled) {}

    public interface Owner {
        boolean isHeld();

        List<Entry> entries();

        void pick(int index);

        default void empty() {}

        default void releaseUnopened() {}
    }

    private record Exit(List<Entry> entries, float[] hover, int picked, long openedAt, long startedAt) {}

    public static final int OPEN_DELAY_TICKS = 5;

    private static final class State {

        @Nullable Owner owner;
        @Nullable List<Entry> entries;
        @Nullable Exit exit;
        int heldTicks;
        boolean spent;
        double cursorX;
        double cursorY;
        float[] hover = new float[0];
        long openedAt;
        int labelIndex = RadialLayout.NONE;
        long labelSince;
        long lastFrameAt;
    }

    private static final State STATE = new State();

    private RadialMenu() {}

    //region the hold [长按]
    public static void hold(@NotNull Owner next) {
        if (STATE.owner != null) return;

        STATE.owner = next;
        STATE.entries = null;
        STATE.heldTicks = 0;
        STATE.spent = false;
    }

    public static boolean isHolding() { return STATE.owner != null; }

    public static boolean isOpen() { return STATE.entries != null; }

    public static void close() {
        STATE.owner = null;
        STATE.entries = null;
        STATE.exit = null;
    }

    public static int getSelected() {
        return STATE.entries == null ? RadialLayout.NONE
                : RadialLayout.sectorAt(STATE.cursorX, STATE.cursorY, STATE.entries.size(), RadialLayout.INNER_RADIUS);
    }

    public static void steer(double dx, double dy) {
        Window window = Minecraft.getInstance().getWindow();
        STATE.cursorX += dx * window.getGuiScaledWidth() / window.getScreenWidth();
        STATE.cursorY += dy * window.getGuiScaledHeight() / window.getScreenHeight();
        double distance = Math.hypot(STATE.cursorX, STATE.cursorY);
        if (distance <= RadialLayout.OUTER_RADIUS) return;

        STATE.cursorX *= RadialLayout.OUTER_RADIUS / distance;
        STATE.cursorY *= RadialLayout.OUTER_RADIUS / distance;
    }

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        Owner current = STATE.owner;
        if (current == null) return;

        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null || minecraft.screen != null) {
            close();
            return;
        }
        if (!current.isHeld()) {
            release(current);
            return;
        }
        if (!STATE.spent && STATE.entries == null && ++STATE.heldTicks >= OPEN_DELAY_TICKS) open(current);
    }

    private static void open(Owner current) {
        List<Entry> listed = List.copyOf(current.entries());
        if (listed.isEmpty()) {
            STATE.spent = true;
            current.empty();
            return;
        }
        STATE.entries = listed;
        STATE.exit = null;
        STATE.cursorX = 0.0D;
        STATE.cursorY = 0.0D;
        STATE.hover = new float[listed.size()];
        STATE.openedAt = Util.getMillis();
        STATE.labelIndex = RadialLayout.NONE;
    }

    private static void release(Owner current) {
        List<Entry> shown = STATE.entries;
        int sector = getSelected();
        STATE.owner = null;
        STATE.entries = null;
        if (shown == null) {
            current.releaseUnopened();
            return;
        }
        boolean picked = sector != RadialLayout.NONE && shown.get(sector).enabled();
        STATE.exit = new Exit(shown, STATE.hover, picked ? sector : RadialLayout.NONE, STATE.openedAt,
                Util.getMillis());
        if (picked) current.pick(sector);
    }

    @SubscribeEvent
    public static void onInteraction(InputEvent.InteractionKeyMappingTriggered event) {
        if (!isHolding()) return;

        event.setCanceled(true);
        event.setSwingHand(false);
    }

    @SubscribeEvent
    public static void onScroll(InputEvent.MouseScrollingEvent event) {
        if (isHolding()) event.setCanceled(true);
    }
    //endregion

    //region drawing [绘制]
    public static void render(@NotNull GuiGraphics graphics) {
        long now = Util.getMillis();
        float seconds = STATE.lastFrameAt == 0L ? 0.0F : Math.min(100L, now - STATE.lastFrameAt) / 1000.0F;
        STATE.lastFrameAt = now;

        List<Entry> shown = STATE.entries;
        if (shown != null) {
            int selected = getSelected();
            ease(STATE.hover, selected, seconds);
            if (selected != STATE.labelIndex) {
                STATE.labelIndex = selected;
                STATE.labelSince = now;
            }
            long sinceOpen = now - STATE.openedAt;
            RadialDrawing.draw(graphics, shown, STATE.hover, sinceOpen, RadialMotion.getOpenScale(sinceOpen),
                    RadialMotion.getOpenAlpha(sinceOpen), RadialLayout.NONE, 0.0F);
            if (STATE.labelIndex != RadialLayout.NONE) {
                float rise = RadialMotion.getLabelIn(now - STATE.labelSince);
                float alpha = RadialMotion.getOpenAlpha(sinceOpen) * rise;
                RadialDrawing.drawLabel(graphics, shown.get(STATE.labelIndex).label(), alpha, rise);
            }
            return;
        }

        Exit leaving = STATE.exit;
        if (leaving == null) return;
        long since = now - leaving.startedAt();
        boolean picked = leaving.picked() != RadialLayout.NONE;
        if (RadialMotion.isExitDone(since, picked)) {
            STATE.exit = null;
            return;
        }
        ease(leaving.hover(), leaving.picked(), seconds);
        long sinceOpen = now - leaving.openedAt();
        float alpha = RadialMotion.getOpenAlpha(sinceOpen) * RadialMotion.getExitAlpha(since, picked);
        RadialDrawing.draw(graphics, leaving.entries(), leaving.hover(), sinceOpen,
                RadialMotion.getOpenScale(sinceOpen) * RadialMotion.getExitScale(since, picked), alpha,
                leaving.picked(), RadialMotion.getFlash(since));
        if (picked) RadialDrawing.drawLabel(graphics, leaving.entries().get(leaving.picked()).label(), alpha, 1.0F);
    }

    private static void ease(float[] weights, int selected, float seconds) {
        for (int i = 0; i < weights.length; i++) {
            weights[i] = RadialMotion.approach(weights[i], i == selected ? 1.0F : 0.0F, seconds);
        }
    }
    //endregion
}
