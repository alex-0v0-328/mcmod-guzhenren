package net.alex.guzhenren.client.radial;

import com.mojang.blaze3d.platform.Window;
import com.mojang.blaze3d.vertex.VertexConsumer;
import java.util.List;
import net.alex.guzhenren.Guzhenren;
import net.alex.guzhenren.client.ModPalette;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.InputEvent;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix4f;

/**
 * The radial menu [扇形选择器]: a held input raises a ring of sectors at the screen's centre, mouse movement points
 * at one, and letting go picks it. Generic: an {@link Owner} says while the input is held, which entries to show and
 * what a pick does; the Amplify Effect Gu [功倍蛊] is the first owner. An {@link Entry} is one sector: its icon, the
 * name at the centre and the caption below the ring while it is pointed at, and whether a pick counts.
 *
 * <p>One hold runs at a time ({@link #hold}). It opens after {@link #OPEN_DELAY_TICKS} held ticks, so a tap never
 * shows it; the entries are read once at that moment and stay fixed until release. Letting go on an enabled sector
 * calls {@link Owner#pick}; inside the inner ring, on a disabled sector or before it opened, nothing happens. With no
 * entries it calls {@link Owner#empty} once and waits out the hold. A screen opening or a logout drops the hold.
 *
 * <p>⚠ While it is open the camera stands still and the walk keys still work: {@code MouseHandlerMixin} hands the
 * mouse movement that would have turned the player to {@link #steer}, in screen pixels, which moves a cursor clamped
 * to the outer ring. The sectors are quads wound like {@code GuiGraphics.fill}'s, so they face the way every other
 * GUI fill does.
 * While a hold runs no interaction key reaches the world and the hotbar does not scroll, so the owner's item stays
 * in hand and nothing is hit or placed by the same click.
 *
 * @author Alex
 * @version 1.0.0
 * @see RadialLayout
 * @since 1.0.0
 */

@EventBusSubscriber(modid = Guzhenren.MOD_ID, value = Dist.CLIENT)
public final class RadialMenu {

    public record Entry(@NotNull ItemStack icon, @NotNull Component label, @NotNull Component detail,
                        boolean enabled) {}

    public interface Owner {
        boolean isHeld();

        List<Entry> entries();

        void pick(int index);

        default void empty() {}
    }

    public static final int OPEN_DELAY_TICKS = 5;
    private static final String CANCEL = "guzhenren.radial.cancel";
    private static final int INNER_RADIUS = 30;
    private static final int OUTER_RADIUS = 80;
    private static final int ICON_RADIUS = 55;
    private static final int ARC_STEPS = 64;
    private static final double GAP = 0.025D;
    private static final int HOVER_FILL = 0x8CFFD54F;
    private static final int DISABLED_SHADE = 0xA0000000;
    private static @Nullable Owner owner;
    private static @Nullable List<Entry> entries;
    private static int heldTicks;
    private static boolean spent;
    private static double cursorX;
    private static double cursorY;

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
    }

    public static int getSelected() {
        return entries == null ? RadialLayout.NONE
                : RadialLayout.sectorAt(cursorX, cursorY, entries.size(), INNER_RADIUS);
    }

    public static void steer(double dx, double dy) {
        Window window = Minecraft.getInstance().getWindow();
        cursorX += dx * window.getGuiScaledWidth() / window.getScreenWidth();
        cursorY += dy * window.getGuiScaledHeight() / window.getScreenHeight();
        double distance = Math.hypot(cursorX, cursorY);
        if (distance <= OUTER_RADIUS) return;

        cursorX *= OUTER_RADIUS / distance;
        cursorY *= OUTER_RADIUS / distance;
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
        cursorX = 0.0D;
        cursorY = 0.0D;
    }

    private static void release(Owner current) {
        List<Entry> shown = entries;
        int sector = getSelected();
        close();
        if (shown != null && sector != RadialLayout.NONE && shown.get(sector).enabled()) current.pick(sector);
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
        List<Entry> shown = entries;
        if (shown == null) return;

        int centerX = graphics.guiWidth() / 2;
        int centerY = graphics.guiHeight() / 2;
        int count = shown.size();
        int selected = getSelected();
        VertexConsumer buffer = graphics.bufferSource().getBuffer(RenderType.gui());
        Matrix4f pose = graphics.pose().last().pose();
        for (int i = 0; i < count; i++) {
            double center = RadialLayout.centerAngle(i, count);
            double half = count == 1 ? Math.PI : Math.PI / count - GAP;
            fillArc(buffer, pose, centerX, centerY, center - half, center + half,
                    i == selected ? HOVER_FILL : ModPalette.PANEL_FILL);
        }
        graphics.flush();

        for (int i = 0; i < count; i++) {
            double angle = RadialLayout.centerAngle(i, count);
            int x = centerX + (int) Math.round(RadialLayout.x(ICON_RADIUS, angle)) - 8;
            int y = centerY + (int) Math.round(RadialLayout.y(ICON_RADIUS, angle)) - 8;
            graphics.renderItem(shown.get(i).icon(), x, y);
            if (!shown.get(i).enabled()) graphics.fill(x, y, x + 16, y + 16, DISABLED_SHADE);
        }

        Font font = Minecraft.getInstance().font;
        if (selected == RadialLayout.NONE) {
            graphics.drawCenteredString(font, Component.translatable(CANCEL), centerX, centerY - 4,
                    ModPalette.LEGEND_TEXT);
        } else {
            Entry entry = shown.get(selected);
            graphics.drawCenteredString(font, entry.label(), centerX, centerY - 4, ModPalette.TEXT);
            graphics.drawCenteredString(font, entry.detail(), centerX, centerY + OUTER_RADIUS + 6, ModPalette.TEXT);
        }
        int dotX = centerX + (int) Math.round(cursorX);
        int dotY = centerY + (int) Math.round(cursorY);
        graphics.fill(dotX - 1, dotY - 1, dotX + 2, dotY + 2, ModPalette.PATH);
    }

    private static void fillArc(VertexConsumer buffer, Matrix4f pose, int centerX, int centerY, double from,
                                double to, int color) {
        int steps = Math.max(2, (int) Math.ceil((to - from) / (2.0D * Math.PI) * ARC_STEPS));
        for (int s = 0; s < steps; s++) {
            double a0 = from + (to - from) * s / steps;
            double a1 = from + (to - from) * (s + 1) / steps;
            vertex(buffer, pose, centerX, centerY, INNER_RADIUS, a0, color);
            vertex(buffer, pose, centerX, centerY, INNER_RADIUS, a1, color);
            vertex(buffer, pose, centerX, centerY, OUTER_RADIUS, a1, color);
            vertex(buffer, pose, centerX, centerY, OUTER_RADIUS, a0, color);
        }
    }

    private static void vertex(VertexConsumer buffer, Matrix4f pose, int centerX, int centerY, double radius,
                               double angle, int color) {
        buffer.addVertex(pose, (float) (centerX + RadialLayout.x(radius, angle)),
                (float) (centerY + RadialLayout.y(radius, angle)), 0.0F).setColor(color);
    }
    //endregion
}
