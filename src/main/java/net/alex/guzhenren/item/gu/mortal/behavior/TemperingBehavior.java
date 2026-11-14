package net.alex.guzhenren.item.gu.mortal.behavior;

import net.alex.guzhenren.gameplay.body.BodyGrade;
import net.alex.guzhenren.item.GuItem.Refusal;
import net.alex.guzhenren.item.gu.GuBehavior;
import net.alex.guzhenren.item.gu.MortalGuItem;
import net.alex.guzhenren.item.gu.mortal.path.metal.BoneBehavior;
import net.alex.guzhenren.item.gu.mortal.path.metal.TendonBehavior;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * 炼体蛊共用行为：把一个部位（皮、筋、骨）炼到某档。
 * <ul>
 *   <li>部位由子类定、档位由注册定；蛊不消耗，用不上可转手；两种玉皮是即时防御蛊，永久档皆为灌注[Channel]
 *       类道痕[Path Marks]蚀刻蛊，一轮即生效，之后门槛拒绝。</li>
 *   <li>门槛问档位本身（{@link BodyGrade#canReplace}）现有档是否让位，拒绝用统一炼体键附现有档名（已指明部位）；
 *       效果是子类 {@link #temper}，经 {@code BodyService} 一次写入，数值与规则全挂在该写入与档位枚举上；
 *       {@code CarvingEffects} 凭此行为找造某档的蛊。</li>
 * </ul>
 *
 * @author Alex
 * @version 1.0.0
 * @see SkinBehavior
 * @see TendonBehavior
 * @see BoneBehavior
 * @since 1.0.0
 */

public abstract class TemperingBehavior<G extends Enum<G> & BodyGrade<G>> implements GuBehavior {

    private static final String FAILED_TAKEN = "guzhenren.item.failed.tempering_taken";
    private final G grade;

    protected TemperingBehavior(G grade) {
        this.grade = grade;
    }

    public @NotNull G getGrade() { return grade; }

    protected abstract @NotNull G getWorn(Player player);

    protected abstract void temper(MortalGuItem gu, ServerPlayer player);

    @Override
    public final @Nullable Refusal checkEffect(MortalGuItem gu, Player player, ItemStack stack) {
        G worn = getWorn(player);
        return grade.canReplace(worn) ? null
                : new Refusal(FAILED_TAKEN, Component.translatable(worn.getTranslationKey()));
    }

    @Override
    public final void applyEffect(MortalGuItem gu, ServerPlayer player, ItemStack stack) { temper(gu, player); }
}
