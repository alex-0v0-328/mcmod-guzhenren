package net.alex.guzhenren.client.hud;

import net.alex.guzhenren.client.ModPalette;
import net.alex.guzhenren.gameplay.aperture.ApertureNourishData;
import net.alex.guzhenren.gameplay.aperture.ApertureNourishService;
import net.alex.guzhenren.gameplay.aperture.ApertureService;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.NotNull;

/**
 * The bar drawn over the hotbar while the aperture is being nourished [温养空窍].
 *
 * <p>Extends {@link net.alex.guzhenren.client.hud.HotbarHud}. Registered above
 * {@code VanillaGuiLayers.AIR_LEVEL}. Reads the cultivation fraction from
 * {@link ApertureNourishService#getFraction} and swaps to a red
 * fill when starving.
 *
 * <p>⚠ It is a separate layer from the charge bar on purpose: that one reads only the held item, and
 * grafting a player attachment into it would take away the property its own header states.
 *
 * @author Alex
 * @version 1.0.0
 * @see net.alex.guzhenren.client.hud.ChargeHud
 * @since 1.0.0
 */

public final class NourishHud extends HotbarHud {

    public static final NourishHud INSTANCE = new NourishHud();

    private NourishHud() {}

    private static final String CAPTION = "guzhenren.hud.nourishing";
    private static final String STARVING = "guzhenren.hud.nourish_starving";
    private static final int FILL_STARVING = 0xFFE57373;

    @Override
    public void render(@NotNull GuiGraphics graphics, @NotNull DeltaTracker delta) {
        Minecraft minecraft = Minecraft.getInstance();
        LocalPlayer player = minecraft.player;
        if (player == null || minecraft.options.hideGui || player.isSpectator()) return;
        if (!ApertureNourishService.isCultivating(player)) return;

        ApertureNourishData nourish = ApertureNourishService.get(player);
        boolean starving = nourish.isStarving();
        int x = (minecraft.getWindow().getGuiScaledWidth() - BAR_WIDTH) / 2;
        int y = barTop(minecraft);

        int target = ApertureNourishService.getTargetIndex(player);
        drawBar(graphics, x, y, ApertureNourishService.getFraction(player, target),
                starving ? FILL_STARVING : ModPalette.APERTURE);

        drawLabel(graphics, minecraft, x, y,
                Component.translatable(starving ? STARVING : CAPTION,
                        ApertureService.getAperture(player, target).nourishProgress()));
    }
}
