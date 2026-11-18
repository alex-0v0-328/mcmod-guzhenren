package net.alex.guzhenren.client.hud;

import net.alex.guzhenren.gameplay.aperture.Talent;
import net.alex.guzhenren.item.GuItem;
import net.alex.guzhenren.refinement.GuRecipeItem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;

/**
 * 蓄力催动蛊虫时快捷栏上方的蓄力进度条：注册在原版氧气条图层之上。
 * <ul>
 *   <li>常规蓄力读取手持 {@link net.alex.guzhenren.item.GuItem} 的 {@code getUseDuration} 与 {@code chargeFraction}：若
 *       物品给出了比例则直接采用，否则回退为原版剩余使用 tick 比例。</li>
 *   <li>开窍[Awakening]仪式期间切换显示资质[Aptitude]进度条：进度跟随 {@link AwakeningHud#getBarBase} 爬升至掷出基
 *       础值，填充中呈灰色、到达后转为默认蓄力色，标题同资质行格式（如「八成七」）。</li>
 *   <li>蓄力蛊[Store Strength Gu]或惯力蛊[Habitual Strength Gu]蓄力期间（{@link StoreStrengthCharge#isCharging}）显示
 *       对应蓄力进度：填充中呈灰色，收到服务端判满（{@link StoreStrengthCharge#isFull}）后转为默认蓄力色，标题在「蓄
 *       力中」与「蓄满」间切换。</li>
 *   <li>研读蛊方（{@link GuRecipeItem}）时显示研读进度，单点阅读的 {@link GuItem#TAP_TICKS} 内不出条，免得一闪
 *       而过。</li>
 *   <li>{@link #isShowing} 是「蓄力条正在显示」的唯一判定入口，{@link NourishHud} 通过它把温养条叠放在本条之上。</li>
 *   <li>⚠ 凡是带使用持续时间的蛊虫都会自动获得本蓄力条，新增蓄力类蛊虫无需修改此类。</li>
 *   <li>当叶子类将标题写成 {@code a / b} 形式时，必须同步给出基于同一对数值计算的 {@code chargeFraction}。</li>
 * </ul>
 */

public final class ChargeHud extends HotbarHud {

    public static final ChargeHud INSTANCE = new ChargeHud();

    private ChargeHud() {}

    private static final int AWAKENING_FILLING_COLOR = 0xFFCCCCCC;
    private static final String STORE_STRENGTH = "guzhenren.hud.store_strength";
    private static final String STORE_STRENGTH_FULL = "guzhenren.hud.store_strength.full";
    private static final String STUDY = "guzhenren.hud.gu_recipe.study";
    static final HudBar AWAKENING = HudBar.of("awakening", BAR_WIDTH, BAR_HEIGHT);

    static boolean isShowing(Minecraft minecraft, LocalPlayer player) {
        if (minecraft.options.hideGui || player.isSpectator()) return false;
        if (AwakeningHud.INSTANCE.getBarBase(player, 0.0F) >= 0) return true;
        if (StoreStrengthCharge.INSTANCE.isCharging()) return true;
        if (!player.isUsingItem()) return false;

        ItemStack stack = player.getUseItem();
        if (stack.getItem() instanceof GuRecipeItem) return getStudied(player) > GuItem.TAP_TICKS;
        return stack.getItem() instanceof GuItem gu && gu.getUseDuration(stack, player) > 0;
    }

    private static int getStudied(LocalPlayer player) {
        return player.getUseItem().getUseDuration(player) - player.getUseItemRemainingTicks();
    }

    @Override
    public void render(@NotNull GuiGraphics graphics) {
        Minecraft minecraft = Minecraft.getInstance();
        LocalPlayer player = minecraft.player;
        if (player == null || !isShowing(minecraft, player)) return;

        int x = getBarLeft(minecraft);
        int y = getBarTop(minecraft);
        float partialTick = minecraft.getTimer().getGameTimeDeltaPartialTick(false);
        int awakening = AwakeningHud.INSTANCE.getBarBase(player, partialTick);
        if (awakening >= 0) {
            int color = AwakeningHud.INSTANCE.isBarFull(player, partialTick) ? GuItem.CHARGE_COLOR_DEFAULT
                    : AWAKENING_FILLING_COLOR;
            draw(graphics, minecraft, AWAKENING, x, y, awakening / 100.0F, color,
                    Talent.formatPercent(awakening));
            return;
        }

        if (StoreStrengthCharge.INSTANCE.isCharging()) {
            boolean full = StoreStrengthCharge.INSTANCE.isFull();
            draw(graphics, minecraft, HOTBAR, x, y, StoreStrengthCharge.INSTANCE.getDisplayProgress(),
                    full ? GuItem.CHARGE_COLOR_DEFAULT : AWAKENING_FILLING_COLOR,
                    Component.translatable(full ? STORE_STRENGTH_FULL : STORE_STRENGTH));
            return;
        }

        ItemStack stack = player.getUseItem();
        if (stack.getItem() instanceof GuRecipeItem) {
            float studied = getStudied(player) / (float) stack.getUseDuration(player);
            draw(graphics, minecraft, HOTBAR, x, y, studied, GuItem.CHARGE_COLOR_DEFAULT,
                    Component.translatable(STUDY));
            return;
        }
        GuItem gu = (GuItem) stack.getItem();
        int total = gu.getUseDuration(stack, player);
        int remaining = player.getUseItemRemainingTicks();
        Float own = gu.chargeFraction(stack, remaining);
        float progress = own != null ? own : 1.0F - remaining / (float) total;
        draw(graphics, minecraft, HOTBAR, x, y, progress, gu.chargeColor(stack, remaining),
                gu.chargeCaption(stack, remaining));
    }
}
