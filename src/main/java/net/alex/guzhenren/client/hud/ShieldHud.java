package net.alex.guzhenren.client.hud;

import java.util.EnumMap;
import java.util.Map;
import net.alex.guzhenren.gameplay.body.ShieldLayer;
import net.alex.guzhenren.gameplay.body.ShieldService;
import net.alex.guzhenren.gameplay.body.ShieldType;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import org.jetbrains.annotations.NotNull;

/**
 * 快捷栏上方的护盾进度条：护盾存在时画一条。
 * <ul>
 *   <li>多层叠加只画一条（Alex 定）：显示最外一层（受击优先消耗、列表里第一个仍在的层），进度比例取它的剩余点数 / 升
 *       起初始点数，打碎后才换下一层；层数大于 1 时在条右上写 {@code x层数}。标题标注护盾种类并采用
 *       {@code ShieldType} 对应颜色；注册在原版氧气条图层之上并读取同步的 {@code ShieldData}，叠在温养条与开窍资质条
 *       上方。</li>
 * </ul>
 */

public final class ShieldHud extends HotbarHud {

    public static final ShieldHud INSTANCE = new ShieldHud();

    private ShieldHud() {}

    private static final String CAPTION = "guzhenren.hud.shield";
    private final Map<ShieldType, Caption> captions = new EnumMap<>(ShieldType.class);

    @Override
    public void render(@NotNull GuiGraphics graphics) {
        Minecraft minecraft = Minecraft.getInstance();
        LocalPlayer player = minecraft.player;
        if (player == null || minecraft.options.hideGui || player.isSpectator()) return;
        if (!ShieldService.isUp(player)) return;

        long now = player.level().getGameTime();
        ShieldLayer outer = null;
        int count = 0;
        for (ShieldLayer layer : ShieldService.get(player).layers()) {
            if (!layer.isUp(now) || layer.max() <= 0.0F) continue;
            if (outer == null) outer = layer;
            count++;
        }
        if (outer == null) return;

        int stacked = (ChargeHud.isAtHotbar(player) ? 1 : 0) + (NourishHud.isShowing(player) ? 1 : 0);
        int x = getBarLeft(minecraft);
        int y = getBarTop(minecraft) - stacked * getStackShift(minecraft);
        draw(graphics, minecraft, HOTBAR, x, y, outer.points() / outer.max(), outer.type().getBarFill(),
                getCaption(outer.type(), Mth.ceil(outer.points()), Mth.ceil(outer.max())));
        if (count > 1) {
            String layers = "x" + count;
            graphics.drawString(minecraft.font, layers, x + BAR_WIDTH - minecraft.font.width(layers),
                    y - TEXT_GAP - minecraft.font.lineHeight, outer.type().getBarFill(), true);
        }
    }

    private Component getCaption(ShieldType type, int points, int max) {
        Caption cached = captions.get(type);
        if (cached == null || cached.points() != points || cached.max() != max) {
            cached = new Caption(points, max, Component.translatable(CAPTION,
                    Component.translatable(type.getTranslationKey()), points, max));
            captions.put(type, cached);
        }
        return cached.text();
    }

    private record Caption(int points, int max, Component text) {}
}
