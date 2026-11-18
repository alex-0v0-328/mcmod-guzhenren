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
 * 炼体蛊（{@link SkinBehavior}、{@link TendonBehavior}、{@link BoneBehavior}）共用的抽象行为基类：将玩家指定身体部位
 * （皮、筋、骨）淬炼至目标档位 {@code G}。
 * <ul>
 *   <li>身体部位由子类指定，目标档位在注册时注入；炼体蛊催动后不消耗物品栈，炼成后可转交他人；两款玉皮属于限时防御蛊，
 *       其余永久皮、筋、骨均为灌注[Channel]型道痕[Path Marks]蚀刻蛊，灌注满一轮即永久生效，此后再次催动会被门槛拒
 *       绝。</li>
 *   <li>{@link #checkEffect} 委托目标档位的 {@link BodyGrade#canReplace} 判定当前部位档位是否允许被替换，被拒时统一返回
 *       附带当前档位译名的拒绝提示；{@link #applyEffect} 调用子类 {@link #temper} 经 {@code BodyService} 一次性写入档
 *       位，具体属性加成与规则全部绑定在档位枚举与写入事件上；{@link CarvingEffects} 通过检索本行为定位赋予某档位的蛊
 *       虫。</li>
 * </ul>
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
