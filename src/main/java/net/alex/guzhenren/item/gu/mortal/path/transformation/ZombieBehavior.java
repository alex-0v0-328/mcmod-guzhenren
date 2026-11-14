package net.alex.guzhenren.item.gu.mortal.path.transformation;

import net.alex.guzhenren.gameplay.aperture.Rank;
import net.alex.guzhenren.gameplay.body.BodyService;
import net.alex.guzhenren.item.GuItem.Refusal;
import net.alex.guzhenren.item.gu.GuBehavior;
import net.alex.guzhenren.item.gu.MortalGuItem;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

/**
 * 僵尸蛊[Zombie Gu]行为：时长取自注册，级别取自转数；二至五转 9 只蛊共用。
 * <ul>
 *   <li>只拒已是僵尸；效果交 {@link BodyService}：临时僵尸化，复发则永久；{@code StateEffects} 凭本行为与
 *       {@link #getRung} 找某级蛊。</li>
 *   <li>⚠ 复发窗口内再催动不拒，即惩罚转永久；该分支提前返回，两种结果共需的逻辑须在其前。</li>
 * </ul>
 *
 * @author Alex
 * @version 1.0.0
 * @since 1.0.0
 */

public record ZombieBehavior(int tempZombieTicks) implements GuBehavior {


    public static int getRung(MortalGuItem gu) { return gu.getRank().ordinal() - Rank.TWO.ordinal(); }

    @Override
    public @Nullable Refusal checkEffect(MortalGuItem gu, Player player, ItemStack stack) {
        return BodyService.isZombie(player) ? new Refusal(Refusal.CANNOT_USE) : null;
    }

    @Override
    public void applyEffect(MortalGuItem gu, ServerPlayer player, ItemStack stack) {
        if (BodyService.isWithinRelapseWindow(player)) {
            BodyService.turnZombie(player, getRung(gu));
            return;
        }
        BodyService.enterTempZombie(player, getRung(gu), tempZombieTicks);
    }
}
