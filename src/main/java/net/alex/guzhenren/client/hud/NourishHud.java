package net.alex.guzhenren.client.hud;

import net.alex.guzhenren.client.ModPalette;
import net.alex.guzhenren.gameplay.aperture.ApertureNourishData;
import net.alex.guzhenren.gameplay.aperture.ApertureNourishService;
import net.alex.guzhenren.gameplay.aperture.ApertureService;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.NotNull;

/**
 * 温养[Nourish]空窍[Aperture]时快捷栏上方的温养条。
 * <ul>
 *   <li>注册在原版氧气条层之上；进度取 {@link ApertureNourishService#getFraction}，真元[Essence]将尽时填红。</li>
 *   <li>蓄力条同显时上移一个 {@link #getStackShift} 免重叠；{@link ShieldHud} 借 {@link #isShowing} 同样叠在其上。</li>
 *   <li>⚠ 故意与蓄力条分层：蓄力条只读手持物品，塞入玩家附件会破坏其类注释所述性质。</li>
 * </ul>
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
    static final HudBar NOURISH = HudBar.of("nourish", BAR_WIDTH, BAR_HEIGHT);
    private boolean captionStarving;
    private int captionProgress = -1;
    private Component caption = Component.empty();

    static boolean isShowing(LocalPlayer player) {
        return !player.isSpectator() && ApertureNourishService.isCultivating(player);
    }

    @Override
    public void render(@NotNull GuiGraphics graphics) {
        Minecraft minecraft = Minecraft.getInstance();
        LocalPlayer player = minecraft.player;
        if (player == null || minecraft.options.hideGui || !isShowing(player)) return;

        ApertureNourishData nourish = ApertureNourishService.get(player);
        boolean starving = nourish.isStarving();
        int x = getBarLeft(minecraft);
        int y = getBarTop(minecraft) - (ChargeHud.isShowing(minecraft, player) ? getStackShift(minecraft) : 0);

        int target = ApertureNourishService.getTargetIndex(player);
        draw(graphics, minecraft, NOURISH, x, y, ApertureNourishService.getFraction(player, target),
                starving ? FILL_STARVING : ModPalette.APERTURE,
                getCaption(starving, ApertureService.getAperture(player, target).nourishProgress()));
    }

    private Component getCaption(boolean starving, int progress) {
        if (starving != captionStarving || progress != captionProgress) {
            caption = Component.translatable(starving ? STARVING : CAPTION, progress);
            captionStarving = starving;
            captionProgress = progress;
        }
        return caption;
    }
}
