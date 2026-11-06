package net.alex.guzhenren.client.slider;

import com.mojang.blaze3d.platform.Window;
import net.alex.guzhenren.Guzhenren;
import net.alex.guzhenren.client.ModPalette;
import net.alex.guzhenren.client.SpriteBatch;
import net.alex.guzhenren.client.hud.HudBar;
import net.alex.guzhenren.client.radial.RadialMenu;
import net.alex.guzhenren.item.GuItem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.InputEvent;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * The hold slider [长按滑条]: a held input raises a horizontal slider under the crosshair, sideways mouse movement
 * picks a value from {@code 0} (cancel) to the owner's top, and letting go picks it. The radial menu's
 * ({@link RadialMenu}) one-value sibling; the Primeval Elder Gu [元老蛊] is its owner.
 *
 * <p>One hold runs at a time ({@link #hold}), and never beside a radial menu's. It opens after
 * {@link RadialMenu#OPEN_DELAY_TICKS} held ticks, so a tap never shows it; the top is read once at that moment and
 * stays fixed until release. It opens on {@code 1}. Letting go on {@code 1} or more calls {@link Owner#pick}; on
 * {@code 0} nothing happens. A top of {@code 0} calls {@link Owner#empty} once and waits out the hold, and letting
 * go then calls nothing: the hold already meant the slider. Letting go before it opens calls
 * {@link Owner#releaseUnopened}. A screen opening or a logout drops the hold with no call.
 *
 * <p>⚠ As with the radial menu, a hold also ends when {@link Owner#isHeld} turns false for any other reason, and
 * {@code pick} is called all the same, so an owner that sends anything checks its item is still in hand first.
 *
 * <p>⚠ While it is open the camera stands still and the walk keys still work: {@code MouseHandlerMixin} hands the
 * horizontal mouse movement to {@link #steer}, in screen pixels, which moves an unseen cursor along the
 * {@link #TRACK_WIDTH}-pixel track, split evenly among the {@code top + 1} values. While a hold runs no interaction
 * key reaches the world and the wheel neither scrolls the hotbar nor moves the slider.
 *
 * <p>⚠ The track is the {@code slider} {@link HudBar} and the knob the {@code hud/slider_knob} sprite, both unscaled;
 * {@code HudBarTexturesTest} pins them to {@link #TRACK_WIDTH}, {@link #TRACK_HEIGHT} and the knob size.
 *
 * @author Alex
 * @version 1.0.0
 * @see RadialMenu
 * @since 1.0.0
 */

@EventBusSubscriber(modid = Guzhenren.MOD_ID, value = Dist.CLIENT)
public final class HoldSlider {

    public interface Owner {
        boolean isHeld();

        int top();

        Component label(int value);

        Component detail(int top);

        void pick(int value);

        default void empty() {}

        default void releaseUnopened() {}
    }

    public static final int TRACK_WIDTH = 120;
    public static final int TRACK_HEIGHT = 4;
    public static final int KNOB_WIDTH = 3;
    public static final int KNOB_HEIGHT = 10;
    private static final int TOP_GAP = 16;
    private static final int LINE_GAP = 3;
    private static final int END_GAP = 4;
    private static final HudBar TRACK = HudBar.of("slider", TRACK_WIDTH, TRACK_HEIGHT);
    private static final ResourceLocation KNOB = Guzhenren.id("hud/slider_knob");
    private static final Component CANCEL = Component.translatable("guzhenren.hud.slider.cancel");
    private static @Nullable Owner owner;
    private static int top = -1;
    private static int heldTicks;
    private static boolean spent;
    private static double cursorX;

    private HoldSlider() {}

    //region the hold [长按]
    public static void hold(@NotNull Owner next) {
        if (owner != null || RadialMenu.isHolding()) return;

        owner = next;
        top = -1;
        heldTicks = 0;
        spent = false;
    }

    public static boolean isHolding() { return owner != null; }

    public static boolean isOpen() { return top > 0; }

    public static void close() {
        owner = null;
        top = -1;
    }

    public static int getValue() {
        if (!isOpen()) return 0;
        return Math.min(top, (int) (cursorX / TRACK_WIDTH * (top + 1)));
    }

    public static void steer(double dx) {
        Window window = Minecraft.getInstance().getWindow();
        cursorX = Mth.clamp(cursorX + dx * window.getGuiScaledWidth() / window.getScreenWidth(), 0.0D, TRACK_WIDTH);
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
        if (!spent && top < 0 && ++heldTicks >= RadialMenu.OPEN_DELAY_TICKS) open(current);
    }

    private static void open(Owner current) {
        int read = current.top();
        if (read <= 0) {
            spent = true;
            current.empty();
            return;
        }
        top = read;
        cursorX = 1.5D * TRACK_WIDTH / (read + 1);
    }

    private static void release(Owner current) {
        boolean opened = isOpen();
        int value = getValue();
        close();
        if (!opened) {
            if (!spent) current.releaseUnopened();
            return;
        }
        if (value > 0) current.pick(value);
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
        Owner current = owner;
        if (current == null || !isOpen()) return;

        Font font = Minecraft.getInstance().font;
        int value = getValue();
        int left = (graphics.guiWidth() - TRACK_WIDTH) / 2;
        int labelY = graphics.guiHeight() / 2 + TOP_GAP;
        int trackY = labelY + font.lineHeight + LINE_GAP + (KNOB_HEIGHT - TRACK_HEIGHT) / 2;
        int detailY = trackY + TRACK_HEIGHT + (KNOB_HEIGHT - TRACK_HEIGHT) / 2 + LINE_GAP;

        Component label = value > 0 ? current.label(value) : CANCEL;
        graphics.drawCenteredString(font, label, graphics.guiWidth() / 2, labelY, ModPalette.TEXT);

        int knob = left + Math.round((float) value / top * (TRACK_WIDTH - KNOB_WIDTH));
        int knobY = trackY - (KNOB_HEIGHT - TRACK_HEIGHT) / 2;
        SpriteBatch.draw(graphics, sprites -> {
            TRACK.draw(sprites, left, trackY, knob - left, GuItem.CHARGE_COLOR_DEFAULT);
            sprites.blit(KNOB, knob, knobY, KNOB_WIDTH, KNOB_HEIGHT);
        });

        int endY = trackY + TRACK_HEIGHT / 2 - font.lineHeight / 2;
        graphics.drawString(font, CANCEL, left - END_GAP - font.width(CANCEL), endY,
                value == 0 ? ModPalette.TEXT : ModPalette.LEGEND_TEXT, true);
        graphics.drawString(font, String.valueOf(top), left + TRACK_WIDTH + END_GAP, endY,
                value == top ? ModPalette.TEXT : ModPalette.LEGEND_TEXT, true);
        graphics.drawCenteredString(font, current.detail(top), graphics.guiWidth() / 2, detailY,
                ModPalette.LEGEND_TEXT);
    }
    //endregion
}
