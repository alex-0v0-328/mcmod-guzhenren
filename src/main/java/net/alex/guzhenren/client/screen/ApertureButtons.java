package net.alex.guzhenren.client.screen;

import java.util.ArrayList;
import java.util.List;
import net.alex.guzhenren.client.ModPalette;
import net.alex.guzhenren.client.SpriteBatch;
import net.alex.guzhenren.gameplay.aperture.ApertureData;
import net.alex.guzhenren.gameplay.aperture.ApertureNourishService;
import net.alex.guzhenren.gameplay.aperture.ApertureService;
import net.alex.guzhenren.network.ClientIntents;
import net.alex.guzhenren.network.payload.ImpactApertureWallPayload;
import net.alex.guzhenren.network.payload.NourishAction;
import net.alex.guzhenren.network.payload.NourishAperturePayload;
import net.alex.guzhenren.network.payload.OpenApertureStoragePayload;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.network.PacketDistributor;

/**
 * B 面板每个空窍[Aperture]下方的按钮。
 * <ul>
 *   <li>上温养[Nourish]空窍、下空窍存储；第一空窍可冲击窍壁时温养换成冲刷窍壁；付不起消耗的按钮画成失效。</li>
 *   <li>温养按钮是当前目标时点亮、框内填进度，再点即取消；开始温养关闭面板。</li>
 *   <li>{@link #draw}、{@link #fillProgress}、{@link #drawLabel} 分开，面板才能一批内先画全部 sprite，再填色，
 *       最后文字；按钮列位置由面板定；标签只建一次。</li>
 * </ul>
 *
 * @author Alex
 * @version 1.0.0
 * @see PlayerInfoScreen
 * @since 1.0.0
 */

final class ApertureButtons {

    private ApertureButtons() {}

    private static final int PROGRESS = 0x804FC3F7;
    private static final int BUTTON_FRAME = 2;
    private static final Component LABEL_NOURISH = Component.translatable("guzhenren.screen.nourish");
    private static final Component LABEL_NOURISH_STOP = Component.translatable("guzhenren.screen.nourish_stop");
    private static final Component LABEL_NOURISH_SECOND = Component.translatable("guzhenren.screen.nourish_second");
    private static final Component LABEL_IMPACT = Component.translatable("guzhenren.screen.impact");
    private static final Component LABEL_STORAGE = Component.translatable("guzhenren.screen.button.storage");

    enum Kind { NOURISH, IMPACT, STORAGE }

    record StackButton(int aperture, Kind kind, Component label, int top) {}

    record ColumnButton(int aperture, Kind kind, Component label, int x0, int x1, int top) {}

    static List<StackButton> buildStackButtons(LocalPlayer player, int aperture) {
        List<StackButton> buttons = new ArrayList<>();
        int step = PlayerInfoScreen.BTN_H + PlayerInfoScreen.BTN_GAP;
        if (aperture == ApertureData.PRIMARY && ApertureNourishService.canImpact(player)) {
            buttons.add(new StackButton(aperture, Kind.IMPACT, LABEL_IMPACT, 0));
            return withStorage(aperture, buttons, step);
        }
        if (!ApertureNourishService.isAtCeiling(player, aperture)) {
            Component label = isRunning(player, aperture) ? LABEL_NOURISH_STOP
                    : ApertureService.getAperture(player, aperture).second() ? LABEL_NOURISH_SECOND : LABEL_NOURISH;
            buttons.add(new StackButton(aperture, Kind.NOURISH, label, 0));
            return withStorage(aperture, buttons, step);
        }
        return withStorage(aperture, buttons, 0);
    }

    static void draw(SpriteBatch sprites, LocalPlayer player, ColumnButton button, int mouseX, int mouseY) {
        boolean hover = isInBox(mouseX, mouseY, button.x0(), button.x1(), button.top());
        boolean running = button.kind() == Kind.NOURISH && isRunning(player, button.aperture());
        boolean live = switch (button.kind()) {
            case IMPACT -> ApertureNourishService.canAffordImpact(player);
            case NOURISH -> running || ApertureNourishService.canNourish(player, button.aperture());
            case STORAGE -> true;
        };
        boolean lit = button.kind() == Kind.NOURISH ? running : hover;
        sprites.blit(Widgets.getButtonSprite(live, lit), button.x0(), button.top(), button.x1() - button.x0(),
                PlayerInfoScreen.BTN_H);
    }

    static void fillProgress(GuiGraphics graphics, LocalPlayer player, ColumnButton button) {
        if (button.kind() != Kind.NOURISH || !isRunning(player, button.aperture())) return;

        int x0 = button.x0() + BUTTON_FRAME;
        int width = button.x1() - button.x0() - BUTTON_FRAME * 2;
        int done = x0 + Math.round(width * ApertureNourishService.getFraction(player, button.aperture()));
        graphics.fill(x0, button.top() + BUTTON_FRAME, done, button.top() + PlayerInfoScreen.BTN_H - BUTTON_FRAME,
                PROGRESS);
    }

    static void drawLabel(GuiGraphics graphics, Font font, ColumnButton button) {
        int width = button.x1() - button.x0();
        graphics.drawString(font, button.label(), button.x0() + (width - font.width(button.label())) / 2,
                button.top() + (PlayerInfoScreen.BTN_H - font.lineHeight) / 2, ModPalette.TEXT, false);
    }

    static boolean click(LocalPlayer player, ColumnButton button) {
        switch (button.kind()) {
            case IMPACT -> PacketDistributor.sendToServer(ImpactApertureWallPayload.INSTANCE);
            case NOURISH -> {
                if (isRunning(player, button.aperture())) {
                    PacketDistributor.sendToServer(new NourishAperturePayload(
                            NourishAction.CANCEL, ApertureNourishService.getTargetIndex(player)));
                } else if (ApertureNourishService.canNourish(player, button.aperture())) {
                    PacketDistributor.sendToServer(new NourishAperturePayload(
                            NourishAction.START, button.aperture()));
                    return true;
                }
            }
            case STORAGE -> PacketDistributor.sendToServer(new OpenApertureStoragePayload(button.aperture()));
        }
        return false;
    }

    static boolean isInBox(double mouseX, double mouseY, int x0, int x1, int top) {
        return Widgets.isOver(mouseX, mouseY, x0, top, x1 - x0, PlayerInfoScreen.BTN_H);
    }

    private static boolean isRunning(LocalPlayer player, int aperture) {
        return ApertureNourishService.isCultivating(player)
                && ApertureNourishService.getTargetIndex(player) == aperture;
    }

    private static List<StackButton> withStorage(int aperture, List<StackButton> buttons, int top) {
        buttons.add(new StackButton(aperture, Kind.STORAGE, LABEL_STORAGE, top));
        return buttons;
    }
}
