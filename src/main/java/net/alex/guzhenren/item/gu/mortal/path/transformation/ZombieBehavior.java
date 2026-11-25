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
 * 僵尸蛊[Zombie Gu]行为：二至五转共 9 只僵尸系凡蛊共用，临时僵尸化时长在注册时注入，僵尸档位由蛊虫转数派生
 * （{@link #getRung}）。
 * <ul>
 *   <li>{@link #checkEffect} 仅拒绝已处于永久僵尸体质的玩家；{@link #applyEffect} 委托 {@link BodyService} 使玩家进入临
 *       时僵尸化（若在复发窗口内再次催动则转为永久僵尸）；{@code StateEffects} 通过检索本行为与 {@link #getRung} 反查对
 *       应档位的僵尸蛊。</li>
 *   <li>⚠ 在复发窗口（{@link BodyService#isWithinRelapseWindow}）内再次催动不会被门槛拦截，而是直接惩罚转为永久僵尸并
 *       提前返回，因此两条分支共用的前置逻辑必须写在该分支判断之前。</li>
 * </ul>
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
