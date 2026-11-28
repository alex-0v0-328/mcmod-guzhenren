package net.alex.guzhenren.client.hud;

import java.util.ArrayList;
import java.util.List;
import net.alex.guzhenren.client.ApertureSea;
import net.alex.guzhenren.client.GuiBatch;
import net.alex.guzhenren.client.ModPalette;
import net.alex.guzhenren.client.SpriteBatch;
import net.alex.guzhenren.display.ModDisplayText;
import net.alex.guzhenren.gameplay.aperture.Aperture;
import net.alex.guzhenren.gameplay.aperture.ApertureData;
import net.alex.guzhenren.gameplay.aperture.AperturePressureService;
import net.alex.guzhenren.gameplay.aperture.ApertureService;
import net.alex.guzhenren.gameplay.body.BodyData;
import net.alex.guzhenren.gameplay.body.BodyService;
import net.alex.guzhenren.gameplay.soul.SoulData;
import net.alex.guzhenren.gameplay.soul.SoulService;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * 左上角玩家状态 HUD（Alex 定）：真元主珠（32×32）、压在其右下的魂魄子珠（24×24）与右侧铭牌。
 * <ul>
 *   <li>注册在原版快捷栏图层之上（{@link net.alex.guzhenren.client.event.ClientEvents}），在 {@code hideGui}、旁观模
 *       式或 F3 调试界面开启时隐藏。</li>
 *   <li>主珠液面 = 当前真元 / 上限，底部深色一层是精炼真元，液色取 {@link ApertureSea} 的境界配色，珠心写百分比；两只
 *       空窍时主珠竖分两室（左第一、右第二）各画液面；未开窍或六转以上主珠空着、不写数。子珠液面 = 魂魄比例。</li>
 *   <li>铭牌第一行金色写境界称号，第二行写资质与寿元（凡人只写寿元）；十绝体压力倒计时时在子珠右侧加一行朱砂字。措辞
 *       取自 {@link ModDisplayText}，与信息命令一致。</li>
 *   <li>内容只取决于空窍、肉身、魂魄附件与压力倒计时整秒数，仅在 {@link Key} 变化时重建 {@link Layout}，静帧零文字分
 *       配；液色先用填色画，波面与珠框合批一个 {@link SpriteBatch}，文字合批一个 {@link GuiBatch}。</li>
 * </ul>
 */

public final class PlayerStatsHud {

    public static final PlayerStatsHud INSTANCE = new PlayerStatsHud();

    private PlayerStatsHud() {}

    private static final int LEFT = 6;
    private static final int TOP = 6;
    private static final int SUB_X = LEFT + 20;
    private static final int SUB_Y = TOP + 22;
    private static final int PLATE_X = LEFT + OrbDrawing.MAIN + 6;
    private static final int MAIN_WINDOW = OrbDrawing.MAIN - 2 * OrbDrawing.INSET;
    private static final int SUB_WINDOW = OrbDrawing.SUB - 2 * OrbDrawing.INSET;
    private static final int PLATE_DIM = 0xFFD8CCB8;
    private static final OrbDrawing.Liquid SOUL = new OrbDrawing.Liquid(0xFF6A3A80, ModPalette.SOUL, 0xFFE4B8FF,
            0xFFF4E0FF);
    private static final long NO_COUNTDOWN = -1L;
    private @Nullable Key cachedKey;
    private Layout layout = Layout.EMPTY;

    public void render(@NotNull GuiGraphics graphics) {
        Minecraft minecraft = Minecraft.getInstance();
        LocalPlayer player = minecraft.player;
        if (player == null || minecraft.options.hideGui) return;
        if (player.isSpectator() || minecraft.getDebugOverlay().showDebugScreen()) return;

        Font font = minecraft.font;
        Layout current = getLayout(player);
        fillLiquids(graphics, current);
        SpriteBatch.draw(graphics, sprites -> drawSprites(sprites, current));
        GuiBatch.draw(graphics, () -> drawText(graphics, font, current));
    }

    private Layout getLayout(LocalPlayer player) {
        ApertureData data = ApertureService.get(player);
        BodyData body = BodyService.get(player);
        boolean counting = body.isExtreme() && isCountingDown(data.getPrimary());
        long seconds = counting
                ? ModDisplayText.getCountdownSeconds(AperturePressureService.getRemainingTicks(player))
                : NO_COUNTDOWN;
        Key key = new Key(data, body, SoulService.get(player), seconds);
        if (!key.equals(cachedKey)) {
            layout = build(key);
            cachedKey = key;
        }
        return layout;
    }

    private static boolean isCountingDown(Aperture aperture) {
        return aperture.pressure() == Aperture.PRESSURE_COUNTDOWN_START && aperture.pressureDeadlineTick() > 0L;
    }

    //region Layout [布局]
    private static Layout build(Key key) {
        ApertureData data = key.data();
        Aperture primary = data.getPrimary();
        List<Chamber> chambers = new ArrayList<>();
        if (data.hasAperture()) {
            int count = data.count();
            for (int i = 0; i < count; i++) {
                Aperture pool = data.get(i);
                int[] colors = ApertureSea.getColors(pool);
                if (colors == null) continue;
                long max = pool.getMaxEssence();
                chambers.add(new Chamber(i * MAIN_WINDOW / count, (i + 1) * MAIN_WINDOW / count,
                        ratio(pool.currentEssence(), max), ratio(pool.distilledEssence(), max),
                        new OrbDrawing.Liquid(colors[ApertureSea.DEEP], colors[ApertureSea.MID],
                                colors[ApertureSea.LIGHT], colors[ApertureSea.CREST])));
            }
        }
        float soul = ratio(key.soul().currentSoul(), key.soul().maxSoul());
        Component lifespan = Component.translatable("guzhenren.hud.lifespan",
                ModDisplayText.formatHudLifespan(key.body()));
        Component detail = data.hasAperture()
                ? Component.empty().append(ModDisplayText.formatHudAptitude(primary, key.body())).append(" · ")
                .append(lifespan)
                : lifespan;
        Component pressure = key.seconds() == NO_COUNTDOWN ? null
                : Component.translatable("guzhenren.hud.aperture_pressure_cooldown", primary.pressure(),
                ModDisplayText.formatClock(key.seconds()));
        return new Layout(chambers, soul, ModDisplayText.formatRealmTitle(primary), detail, pressure);
    }

    private static float ratio(long current, long max) {
        return max <= 0L ? 0.0F : (float) Math.clamp(current / (double) max, 0.0, 1.0);
    }

    private static String percent(float fraction) { return Integer.toString(Math.round(fraction * 100.0F)); }
    //endregion

    //region Drawing [绘制]
    private static void fillLiquids(GuiGraphics graphics, Layout layout) {
        int x = LEFT + OrbDrawing.INSET;
        int y = TOP + OrbDrawing.INSET;
        for (Chamber chamber : layout.chambers()) {
            OrbDrawing.fillLiquid(graphics, x, y, MAIN_WINDOW, chamber.from(), chamber.to(), chamber.essence(),
                    chamber.refined(), chamber.liquid());
        }
        if (layout.chambers().size() > 1) {
            graphics.fill(x + MAIN_WINDOW / 2, y + 1, x + MAIN_WINDOW / 2 + 1, y + MAIN_WINDOW - 1, ModPalette.SURFACE_0);
        }
        OrbDrawing.fillLiquid(graphics, SUB_X + OrbDrawing.INSET, SUB_Y + OrbDrawing.INSET, SUB_WINDOW, 0,
                SUB_WINDOW, layout.soul(), 0.0F, SOUL);
    }

    private static void drawSprites(SpriteBatch sprites, Layout layout) {
        for (Chamber chamber : layout.chambers()) {
            OrbDrawing.drawWave(sprites, LEFT + OrbDrawing.INSET, TOP + OrbDrawing.INSET, MAIN_WINDOW, chamber.from(),
                    chamber.to(), chamber.essence(), chamber.liquid());
        }
        OrbDrawing.drawFrame(sprites, LEFT, TOP, true);
        OrbDrawing.drawWave(sprites, SUB_X + OrbDrawing.INSET, SUB_Y + OrbDrawing.INSET, SUB_WINDOW, 0, SUB_WINDOW,
                layout.soul(), SOUL);
        OrbDrawing.drawFrame(sprites, SUB_X, SUB_Y, false);
    }

    private static void drawText(GuiGraphics graphics, Font font, Layout layout) {
        int centerY = TOP + (OrbDrawing.MAIN - font.lineHeight) / 2 - 2;
        for (Chamber chamber : layout.chambers()) {
            String text = percent(chamber.essence());
            int centerX = LEFT + OrbDrawing.INSET + (chamber.from() + chamber.to()) / 2;
            graphics.drawString(font, text, centerX - font.width(text) / 2, centerY, ModPalette.TEXT, true);
        }
        graphics.drawString(font, layout.title(), PLATE_X, TOP + 3, ModPalette.ACCENT, true);
        graphics.drawString(font, layout.detail(), PLATE_X, TOP + 13, PLATE_DIM, true);
        int soulX = SUB_X + OrbDrawing.SUB + 2;
        int soulY = SUB_Y + (OrbDrawing.SUB - font.lineHeight) / 2;
        String soul = percent(layout.soul());
        graphics.drawString(font, soul, soulX, soulY, ModPalette.TEXT, true);
        if (layout.pressure() != null) {
            graphics.drawString(font, layout.pressure(), soulX + font.width(soul) + 6, soulY, ModPalette.SHORT, true);
        }
    }
    //endregion

    private record Key(ApertureData data, BodyData body, SoulData soul, long seconds) {}

    private record Chamber(int from, int to, float essence, float refined, OrbDrawing.Liquid liquid) {}

    private record Layout(List<Chamber> chambers, float soul, Component title, Component detail,
                          @Nullable Component pressure) {

        static final Layout EMPTY = new Layout(List.of(), 0.0F, Component.empty(), Component.empty(), null);
    }
}
