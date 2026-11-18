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
 * 希望蛊[Hope Gu]物品：唯一能为玩家开窍[Awakening]的蛊虫，右键单击即启动开窍仪式。
 * <ul>
 *   <li>直接继承 {@link MortalGuItem} 而非 {@code OneShotGuItem}：无需炼化、也不挂载通用行为；右键
 *       单击当场抽取资质[Aptitude]并消耗蛊虫，后续流程交由 {@link AwakeningRitualService} 接管（锁定玩家移动并在闪光达
 *       峰时完成开窍）；若周围存在威胁（{@link AwakeningRitualService#isInDanger}）则拒绝启动。</li>
 *   <li>兼容旧版按住式仪式已写入 {@link ModDataComponents#AWAKEN_BASE} 的预掷资质值，读取后立即移除该组件，以免创造模式
 *       保留的物品栈复用同一资质。</li>
 *   <li>⚠ 堆叠上限由 {@link MortalGuItem} 固定锁为 1：数据组件在同一物品栈上共享，若允许堆叠会导致整组希望蛊共用同一个
 *       预掷资质。</li>
 * </ul>
 */

public class HopeGuItem extends MortalGuItem {

    private static final String FAILED_AWAKENED = "guzhenren.item.failed.awakened";
    private static final String FAILED_AWAKENING = "guzhenren.item.failed.awakening";
    private static final String FAILED_NOURISHING = "guzhenren.item.failed.nourishing";
    private static final String FAILED_DANGER = "guzhenren.item.failed.awakening_danger";

    public HopeGuItem(GuProperties guProperties) {
        super(new Properties(), guProperties);
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
