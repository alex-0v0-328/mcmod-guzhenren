package net.alex.guzhenren.item.gu.mortal.path.human;

import net.alex.guzhenren.gameplay.aperture.ApertureNourishService;
import net.alex.guzhenren.gameplay.aperture.ApertureService;
import net.alex.guzhenren.gameplay.aperture.Talent;
import net.alex.guzhenren.gameplay.aperture.awakening.AwakeningRitualService;
import net.alex.guzhenren.item.gu.GuProperties;
import net.alex.guzhenren.item.gu.MortalGuItem;
import net.alex.guzhenren.registry.item.ModDataComponents;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

/**
 * 希望蛊[Hope Gu]：唯一能开窍[Awakening]的蛊，单击启动开窍仪式。
 * <ul>
 *   <li>直接继承 {@link MortalGuItem} 而非 {@code OneShotGuItem}：无炼化、不跑行为；单击掷资质[Aptitude]、收蛊，余下交
 *       {@link AwakeningRitualService}：定住玩家，闪光达峰时开窍；附近有危险则拒绝
 *       （{@link AwakeningRitualService#isInDanger}）。</li>
 *   <li>旧按住式仪式所掷堆叠仍在 {@link ModDataComponents#AWAKEN_BASE} 存值，仪式沿用；创造模式留堆叠，故清除供重
 *       掷。</li>
 *   <li>⚠ {@code stacksTo(1)}：数据组件整堆共享，可堆叠即 64 份同一资质。</li>
 * </ul>
 *
 * @author Alex
 * @version 1.0.0
 * @see AwakeningRitualService
 * @see net.alex.guzhenren.item.gu.MortalGuItem
 * @since 1.0.0
 */

public class HopeGuItem extends MortalGuItem {

    private static final String FAILED_AWAKENED = "guzhenren.item.failed.awakened";
    private static final String FAILED_AWAKENING = "guzhenren.item.failed.awakening";
    private static final String FAILED_NOURISHING = "guzhenren.item.failed.nourishing";
    private static final String FAILED_DANGER = "guzhenren.item.failed.awakening_danger";

    public HopeGuItem(GuProperties guProperties) {
        super(new Properties().stacksTo(1), guProperties);
    }

    @Override
    protected @Nullable Refusal checkOwnUse(Player player, ItemStack stack) {
        if (ApertureService.isAwakened(player)) return new Refusal(FAILED_AWAKENED);
        if (AwakeningRitualService.isInRitual(player)) return new Refusal(FAILED_AWAKENING);
        if (ApertureNourishService.isCultivating(player)) return new Refusal(FAILED_NOURISHING);
        return AwakeningRitualService.isInDanger(player) ? new Refusal(FAILED_DANGER) : null;
    }

    @Override
    protected final @Nullable Refusal checkUse(Player player, ItemStack stack) { return checkOwnUse(player, stack); }

    @Override
    protected final int useDurationTicks(Player player, ItemStack stack) { return 0; }

    @Override
    protected int getCooldownTicks(ItemStack stack) { return REFINE_DONE_COOLDOWN_TICKS; }

    @Override
    protected final int performUse(ServerPlayer player, ItemStack stack) {
        int rolled = stack.getOrDefault(ModDataComponents.AWAKEN_BASE, 0);
        stack.remove(ModDataComponents.AWAKEN_BASE);
        AwakeningRitualService.start(player, rolled > 0 ? rolled : Talent.randomPercent(Talent.randomTalent()));
        return 1;
    }
}
