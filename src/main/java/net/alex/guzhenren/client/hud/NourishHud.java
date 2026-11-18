package net.alex.guzhenren.client.hud;

import net.alex.guzhenren.client.ModPalette;
import net.alex.guzhenren.client.cinematic.CultivationCamera;
import net.alex.guzhenren.gameplay.aperture.ApertureNourishData;
import net.alex.guzhenren.gameplay.aperture.ApertureNourishService;
import net.alex.guzhenren.gameplay.aperture.ApertureService;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.NotNull;

/**
 * 温养空窍时在快捷栏上方显示的温养进度条。
 * <ul>
 *   <li>注册在原版氧气条图层之上；进度读取 {@link ApertureNourishService#getFraction}，当真元即将耗尽时填充转为红
 *       色。</li>
 *   <li>当蓄力条同时显示时，本条上移一个 {@link #getStackShift} 高度避免重叠；{@link ShieldHud} 同样通过
 *       {@link #isShowing} 将护盾条叠放在本条之上。</li>
 *   <li>温养运镜期间（{@link CultivationCamera#isShowing}）其余 HUD 图层隐藏，温养条连同标题改画在下黑边中央（
 *       {@link CinematicHud}）。</li>
 *   <li>⚠ 本类故意与蓄力条分开独立实现：蓄力条只读取手持物品状态，若强行塞入玩家附件查询会破坏其类约束。</li>
 * </ul>
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
        int y = getTop(minecraft, player);

        int target = ApertureNourishService.getTargetIndex(player);
        draw(graphics, minecraft, NOURISH, x, y, ApertureNourishService.getFraction(player, target),
                starving ? FILL_STARVING : ModPalette.APERTURE,
                getCaption(starving, ApertureService.getAperture(player, target).nourishProgress()));
    }

    private static int getTop(Minecraft minecraft, LocalPlayer player) {
        if (CultivationCamera.isShowing()) {
            return CinematicHud.getBottomCenter(minecraft.getWindow().getGuiScaledHeight())
                    - (BAR_HEIGHT - TEXT_GAP - minecraft.font.lineHeight) / 2;
        }
        return getBarTop(minecraft) - (ChargeHud.isShowing(minecraft, player) ? getStackShift(minecraft) : 0);
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
