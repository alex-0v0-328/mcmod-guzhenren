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
 * 长按呼出的扇形选择器。
 *
 * <ul>
 *   <li>{@link Owner} 定何时算按住、显示哪些条目、选中后做什么；现有持有者：功倍蛊[Amplify Effect Gu]，月刃蛊[Moon
 *       Blade Gu]（选其杀招[Killing Move]）；{@link Entry} 即一个扇区：图标、指向时中心名称、选中是否生效。</li>
 *   <li>按住时屏幕中心升起一圈扇区，指向其一、松开即选；同时只有一次长按（{@link #hold}）；按住
 *       {@link #OPEN_DELAY_TICKS} tick 才打开，轻点不显示；条目打开时读一次，松开前不变。</li>
 *   <li>松开：启用扇区上调 {@link Owner#pick}，内圈或禁用扇区无事；无条目调一次 {@link Owner#empty} 后等结束；未打开环
 *       （打开前或 {@code empty} 后）调 {@link Owner#releaseUnopened}，默认空操作；开界面或登出即丢弃、不调方法。</li>
 *   <li>⚠ {@link Owner#isHeld} 因其他原因变 false（如物品离手）同样结束长按，照常调 {@code pick} 与
 *       {@code releaseUnopened}；会发包的持有者须先确认物品仍在手上。</li>
 *   <li>⚠ 打开期间镜头不动、行走照常：{@code MouseHandlerMixin} 把本应转动玩家的鼠标位移按屏幕像素交给 {@link #steer}，
 *       移动一个不可见、限在外圈内的光标，只由被指向的扇区体现。</li>
 *   <li>长按期间交互键不传到世界、快捷栏不滚动，物品留在手上，同一次点击不击打或放置。</li>
 *   <li>按挂钟逐帧跟随 {@link RadialMotion}，由 {@link RadialDrawing} 绘制；松开立即结束长按（镜头恢复、发送选择），只
 *       留画环离场的 {@code Exit}。</li>
 * </ul>
 *
 * @author Alex
 * @version 1.0.0
 * @see RadialLayout
 * @see RadialMotion
 * @see RadialDrawing
 * @since 1.0.0
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
