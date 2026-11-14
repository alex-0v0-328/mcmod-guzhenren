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
 * 护盾在时快捷栏上方的护盾条，每层一条。
 * <ul>
 *   <li>进度为剩余 / 升起点数，标种类，色取 {@code ShieldType}；注册在原版氧气条层上，读同步的 {@code ShieldData}；
 *       最新层（受击先耗）在最下；整组叠在蓄力、温养条上，每显示一条上移 {@link #getStackShift}。</li>
 * </ul>
 *
 * @author Alex
 * @version 1.0.0
 * @see ShieldService
 * @since 1.0.0
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
