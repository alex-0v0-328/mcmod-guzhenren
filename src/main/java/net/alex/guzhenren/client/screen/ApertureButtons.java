package net.alex.guzhenren.client.screen;

import java.util.ArrayList;
import java.util.List;
import net.alex.guzhenren.client.ModPalette;
import net.alex.guzhenren.client.SpriteBatch;
import net.alex.guzhenren.gameplay.aperture.ApertureData;
import net.alex.guzhenren.gameplay.aperture.ApertureNourishService;
import net.alex.guzhenren.gameplay.aperture.ApertureService;
import net.alex.guzhenren.network.payload.ApertureNourishAction;
import net.alex.guzhenren.network.payload.ApertureNourishPayload;
import net.alex.guzhenren.network.payload.ImpactApertureWallPayload;
import net.alex.guzhenren.network.payload.OpenApertureStoragePayload;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.network.PacketDistributor;

/**
 * B 面板空窍页签中单个空窍对应的一组操作按钮控件。
 * <ul>
 *   <li>上方按钮为温养[Nourish]空窍、下方按钮为打开空窍存储；当第一空窍可冲击窍壁时，温养按钮切换为「冲刷窍壁」；
 *       无法支付对应消耗的按钮绘制为失效状态。</li>
 *   <li>若某空窍正处于温养目标状态，则对应温养按钮保持点亮并在框内填充当前进度，再次点击即取消温养；发起新温养时会
 *       自动关闭信息面板。</li>
 *   <li>将 {@link #draw}、{@link #fillProgress} 与 {@link #drawLabel} 拆分为三个阶段，以便外层面板在一个批次内先统一
 *       绘制全部精灵图、再填充进度色块、最后绘制全部文本；按钮栏坐标与悬停命中判定由面板掌控，标签组件在构造时仅创建一
 *       次。</li>
 *   <li>按钮栏宽度取 {@link #getLabelWidth} 测得的全部候选标签最大宽度，防止标签文案切换时栏宽跳动；当面板空间受限
 *       压窄按钮栏时，过长标签自动截断为省略号，并通过 {@link #isClipped} 通知面板在悬停时补全完整文字提示（英文界面
 *       在 GUI 6 缩放下会触发截断）。</li>
 * </ul>
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

    static int getLabelWidth(Font font) {
        int width = 0;
        for (Component label : List.of(LABEL_NOURISH, LABEL_NOURISH_STOP, LABEL_NOURISH_SECOND, LABEL_IMPACT,
                LABEL_STORAGE)) {
            width = Math.max(width, font.width(label));
        }
        return width;
    }

    static void draw(SpriteBatch sprites, LocalPlayer player, ColumnButton button, boolean hover) {
        boolean running = button.kind() == Kind.NOURISH && isRunning(player, button.aperture());
        boolean live = switch (button.kind()) {
            case IMPACT -> ApertureNourishService.canAffordImpact(player);
            case NOURISH -> running || ApertureNourishService.canNourish(player, button.aperture());
            case STORAGE -> true;
        };
        boolean lit = button.kind() == Kind.NOURISH ? running : hover;
        sprites.blit(Widgets.ui(!live ? "button_disabled" : lit ? "button_highlighted" : "button"), button.x0(),
                button.top(), button.x1() - button.x0(), PlayerInfoScreen.BTN_H);
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
        int y = button.top() + (PlayerInfoScreen.BTN_H - font.lineHeight) / 2;
        if (!isClipped(font, button)) {
            int width = button.x1() - button.x0();
            graphics.drawString(font, button.label(), button.x0() + (width - font.width(button.label())) / 2, y,
                    ModPalette.TEXT, false);
            return;
        }
        graphics.drawString(font, InfoRowRenderer.clip(font, button.label(), getLabelRoom(button)),
                button.x0() + BUTTON_FRAME * 2, y, ModPalette.TEXT, false);
    }

    static boolean isClipped(Font font, ColumnButton button) {
        return font.width(button.label()) > getLabelRoom(button);
    }

    private static int getLabelRoom(ColumnButton button) { return button.x1() - button.x0() - BUTTON_FRAME * 4; }

    static boolean click(LocalPlayer player, ColumnButton button) {
        switch (button.kind()) {
            case IMPACT -> PacketDistributor.sendToServer(ImpactApertureWallPayload.INSTANCE);
            case NOURISH -> {
                if (isRunning(player, button.aperture())) {
                    PacketDistributor.sendToServer(new ApertureNourishPayload(
                            ApertureNourishAction.CANCEL, ApertureNourishService.getTargetIndex(player)));
                } else if (ApertureNourishService.canNourish(player, button.aperture())) {
                    PacketDistributor.sendToServer(new ApertureNourishPayload(
                            ApertureNourishAction.START, button.aperture()));
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
