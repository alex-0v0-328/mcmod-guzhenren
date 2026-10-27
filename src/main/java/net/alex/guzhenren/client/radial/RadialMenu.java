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
 * The radial menu [扇形选择器]: a held input raises a ring of sectors at the screen's centre, mouse movement points
 * at one, and letting go picks it. Generic: an {@link Owner} says while the input is held, which entries to show and
 * what a pick does; the Amplify Effect Gu [功倍蛊] is the first owner. An {@link Entry} is one sector: its icon, the
 * name shown at the centre while it is pointed at, and whether a pick counts.
 *
 * <p>One hold runs at a time ({@link #hold}). It opens after {@link #OPEN_DELAY_TICKS} held ticks, so a tap never
 * shows it; the entries are read once at that moment and stay fixed until release. Letting go on an enabled sector
 * calls {@link Owner#pick}; inside the inner ring, on a disabled sector or before it opened, nothing happens. With no
 * entries it calls {@link Owner#empty} once and waits out the hold. A screen opening or a logout drops the hold.
 *
 * <p>⚠ While it is open the camera stands still and the walk keys still work: {@code MouseHandlerMixin} hands the
 * mouse movement that would have turned the player to {@link #steer}, in screen pixels, which moves an unseen cursor
 * clamped to the outer ring; only the pointed sector shows it. While a hold runs no interaction key reaches the world
 * and the hotbar does not scroll, so the owner's item stays in hand and nothing is hit or placed by the same click.
 *
 * <p>The animation follows {@link RadialMotion} on the wall clock, frame by frame, and {@link RadialDrawing} draws
 * each frame. Letting go ends the hold at once (the camera turns again, the pick is sent) and leaves an {@code Exit}
 * that only draws the ring leaving.
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
    }

    private record Exit(List<Entry> entries, float[] hover, int picked, long openedAt, long startedAt) {}

    public static final int OPEN_DELAY_TICKS = 5;
    private static @Nullable Owner owner;
    private static @Nullable List<Entry> entries;
    private static @Nullable Exit exit;
    private static int heldTicks;
    private static boolean spent;
    private static double cursorX;
    private static double cursorY;
    private static float[] hover = new float[0];
    private static long openedAt;
    private static int labelIndex = RadialLayout.NONE;
    private static long labelSince;
    private static long lastFrameAt;

    private RadialMenu() {}

    //region the hold
    public static void hold(@NotNull Owner next) {
        if (owner != null) return;

        owner = next;
        entries = null;
        heldTicks = 0;
        spent = false;
    }

    public static boolean isHolding() { return owner != null; }

    public static boolean isOpen() { return entries != null; }

    public static void close() {
        owner = null;
        entries = null;
        exit = null;
    }

    public static int getSelected() {
        return entries == null ? RadialLayout.NONE
                : RadialLayout.sectorAt(cursorX, cursorY, entries.size(), RadialLayout.INNER_RADIUS);
    }

    public static void steer(double dx, double dy) {
        Window window = Minecraft.getInstance().getWindow();
        cursorX += dx * window.getGuiScaledWidth() / window.getScreenWidth();
        cursorY += dy * window.getGuiScaledHeight() / window.getScreenHeight();
        double distance = Math.hypot(cursorX, cursorY);
        if (distance <= RadialLayout.OUTER_RADIUS) return;

        cursorX *= RadialLayout.OUTER_RADIUS / distance;
        cursorY *= RadialLayout.OUTER_RADIUS / distance;
    }

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        Owner current = owner;
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
        if (!spent && entries == null && ++heldTicks >= OPEN_DELAY_TICKS) open(current);
    }

    private static void open(Owner current) {
        List<Entry> listed = List.copyOf(current.entries());
        if (listed.isEmpty()) {
            spent = true;
            current.empty();
            return;
        }
        entries = listed;
        exit = null;
        cursorX = 0.0D;
        cursorY = 0.0D;
        hover = new float[listed.size()];
        openedAt = Util.getMillis();
        labelIndex = RadialLayout.NONE;
    }

    private static void release(Owner current) {
        List<Entry> shown = entries;
        int sector = getSelected();
        owner = null;
        entries = null;
        if (shown == null) return;

        boolean picked = sector != RadialLayout.NONE && shown.get(sector).enabled();
        exit = new Exit(shown, hover, picked ? sector : RadialLayout.NONE, openedAt, Util.getMillis());
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

    //region drawing
    public static void render(@NotNull GuiGraphics graphics) {
        long now = Util.getMillis();
        float seconds = lastFrameAt == 0L ? 0.0F : Math.min(100L, now - lastFrameAt) / 1000.0F;
        lastFrameAt = now;

        List<Entry> shown = entries;
        if (shown != null) {
            int selected = getSelected();
            ease(hover, selected, seconds);
            if (selected != labelIndex) {
                labelIndex = selected;
                labelSince = now;
            }
            long sinceOpen = now - openedAt;
            RadialDrawing.draw(graphics, shown, hover, sinceOpen, RadialMotion.getOpenScale(sinceOpen),
                    RadialMotion.getOpenAlpha(sinceOpen), RadialLayout.NONE, 0.0F);
            if (labelIndex != RadialLayout.NONE) {
                float rise = RadialMotion.getLabelIn(now - labelSince);
                float alpha = RadialMotion.getOpenAlpha(sinceOpen) * rise;
                RadialDrawing.drawLabel(graphics, shown.get(labelIndex).label(), alpha, rise);
            }
            return;
        }

        Exit leaving = exit;
        if (leaving == null) return;
        long since = now - leaving.startedAt();
        boolean picked = leaving.picked() != RadialLayout.NONE;
        if (RadialMotion.isExitDone(since, picked)) {
            exit = null;
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
