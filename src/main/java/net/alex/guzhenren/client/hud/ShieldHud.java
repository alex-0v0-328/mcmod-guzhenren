package net.alex.guzhenren.client.hud;

import net.alex.guzhenren.gameplay.body.BodyService;
import net.alex.guzhenren.gameplay.body.ShieldLayer;
import net.alex.guzhenren.gameplay.body.ShieldService;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import org.jetbrains.annotations.NotNull;

/**
 * The bars drawn over the hotbar while shields [护盾] are up: one per layer, its points left over the points it was
 * raised with, captioned with its kind.
 *
 * <p>Extends {@link net.alex.guzhenren.client.hud.HotbarHud}. Registered above the vanilla air-level layer. Reads the
 * synced {@code ShieldData}; each fill is its {@code ShieldType}'s. The newest layer, the one a hit spends first,
 * sits lowest; the bars stack above the charge and nourish bars, one {@link #getStackShift} for each that is up.
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

    @Override
    public void render(@NotNull GuiGraphics graphics) {
        Minecraft minecraft = Minecraft.getInstance();
        LocalPlayer player = minecraft.player;
        if (player == null || minecraft.options.hideGui || player.isSpectator()) return;
        if (!ShieldService.isUp(player)) return;

        long now = BodyService.getGameTime(player);
        int stacked = (ChargeHud.isShowing(minecraft, player) ? 1 : 0) + (NourishHud.isShowing(player) ? 1 : 0);
        int x = (minecraft.getWindow().getGuiScaledWidth() - BAR_WIDTH) / 2;
        int y = getBarTop(minecraft) - stacked * getStackShift(minecraft);
        for (ShieldLayer layer : ShieldService.get(player).layers()) {
            if (!layer.isUp(now) || layer.max() <= 0.0F) continue;
            draw(graphics, minecraft, HOTBAR, x, y, layer.points() / layer.max(), layer.type().getBarFill(),
                    Component.translatable(CAPTION, Component.translatable(layer.type().getTranslationKey()),
                            Mth.ceil(layer.points()), Mth.ceil(layer.max())));
            y -= getStackShift(minecraft);
        }
    }
}
