package net.alex.guzhenren.client.hud;

import net.alex.guzhenren.gameplay.body.MugwortShieldData;
import net.alex.guzhenren.gameplay.body.MugwortShieldService;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import org.jetbrains.annotations.NotNull;

/**
 * The bar drawn over the hotbar while the Heavenly Mugwort shield [天蓬护盾] is up: the points left over the points
 * it was raised with.
 *
 * <p>Extends {@link net.alex.guzhenren.client.hud.HotbarHud}. Registered above the vanilla air-level layer. Reads the
 * synced {@code MugwortShieldData}; the fill is the Gu's own cream. It stacks above the charge and nourish bars, one
 * {@link #getStackShift} for each that is up.
 *
 * @author Alex
 * @version 1.0.0
 * @see MugwortShieldService
 * @since 1.0.0
 */

public final class MugwortShieldHud extends HotbarHud {

    public static final MugwortShieldHud INSTANCE = new MugwortShieldHud();

    private MugwortShieldHud() {}

    private static final String CAPTION = "guzhenren.hud.mugwort_shield";
    private static final int FILL = 0xFFE6E0D2;

    @Override
    public void render(@NotNull GuiGraphics graphics) {
        Minecraft minecraft = Minecraft.getInstance();
        LocalPlayer player = minecraft.player;
        if (player == null || minecraft.options.hideGui || player.isSpectator()) return;
        if (!MugwortShieldService.isUp(player)) return;

        MugwortShieldData shield = MugwortShieldService.get(player);
        int stacked = (ChargeHud.isShowing(minecraft, player) ? 1 : 0) + (NourishHud.isShowing(player) ? 1 : 0);
        int x = (minecraft.getWindow().getGuiScaledWidth() - BAR_WIDTH) / 2;
        int y = getBarTop(minecraft) - stacked * getStackShift(minecraft);

        draw(graphics, minecraft, x, y, shield.points() / shield.max(), FILL,
                Component.translatable(CAPTION, Mth.ceil(shield.points()), Mth.ceil(shield.max())));
    }
}
