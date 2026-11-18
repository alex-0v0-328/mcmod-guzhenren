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
 * 快捷栏上方的护盾进度条：护盾存在时为每一层独立绘制一条进度条。
 * <ul>
 *   <li>进度比例取剩余点数 / 升起初始点数，标题标注护盾种类并采用 {@code ShieldType} 对应颜色；注册在原版氧气条图层之上
 *       并读取同步的 {@code ShieldData}；最新一层（受击优先消耗）置于最下方，整组叠在蓄力条与温养条上方，每显示一层上
 *       移一个 {@link #getStackShift}。</li>
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
        int stacked = (ChargeHud.isShowing(minecraft, player) ? 1 : 0) + (NourishHud.isShowing(player) ? 1 : 0);
        int x = getBarLeft(minecraft);
        int y = getBarTop(minecraft) - stacked * getStackShift(minecraft);
        for (ShieldLayer layer : ShieldService.get(player).layers()) {
            if (!layer.isUp(now) || layer.max() <= 0.0F) continue;
            draw(graphics, minecraft, HOTBAR, x, y, layer.points() / layer.max(), layer.type().getBarFill(),
                    getCaption(layer.type(), Mth.ceil(layer.points()), Mth.ceil(layer.max())));
            y -= getStackShift(minecraft);
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
