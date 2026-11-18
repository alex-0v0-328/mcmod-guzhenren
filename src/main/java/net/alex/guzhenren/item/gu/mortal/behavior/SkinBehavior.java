package net.alex.guzhenren.item.gu.mortal.behavior;

import net.alex.guzhenren.gameplay.body.BodyService;
import net.alex.guzhenren.gameplay.body.Skin;
import net.alex.guzhenren.gameplay.body.SkinService;
import net.alex.guzhenren.item.gu.MortalGuItem;
import net.alex.guzhenren.item.gu.mortal.path.rule.AmplifyEffectGuItem;
import net.alex.guzhenren.item.gu.mortal.path.rule.AmplifyEffectGuItem.Amplification;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import org.jetbrains.annotations.NotNull;

/**
 * 皮蛊炼体行为：涵盖限时的玉皮蛊[Jade Skin Gu]、白玉蛊[White Jade Gu]与永久的铜皮蛊[Bronze Skin Gu]、
 * 铁皮蛊[Iron Skin Gu]。
 * <ul>
 *   <li>⚠ 皮肤档位与限时时长在注册时注入，构造器强制校验「限时皮必须给正数时长、永久皮时长必须为 0」。</li>
 *   <li>通过 {@link BodyService#wearSkin} 写入皮肤后立即调用 {@link SkinService#tickSkin} 投影状态，使 HUD 无需等待下一
 *       轮心跳即可显示玉皮；减伤、倒计时与受击闪光均随本次写入生效。</li>
 *   <li>功倍蛊[Amplify Effect Gu]仅倍增限时皮的持续时长，永久皮不接受增幅也不触发功倍蛊扣费；{@link TimedEffects} 通过
 *       检索本行为反查赋予当前限时皮的蛊虫。</li>
 * </ul>
 */

public final class SkinBehavior extends TemperingBehavior<Skin> {

    private final int durationTicks;

    public SkinBehavior(Skin skin, int durationTicks) {
        super(skin);
        if (skin.isTimed() != durationTicks > 0) {
            throw new IllegalArgumentException(skin + " takes " + (skin.isTimed() ? "a duration" : "no duration"));
        }
        this.durationTicks = durationTicks;
    }

    public int getDurationTicks() { return durationTicks; }

    @Override
    public boolean canBeAmplified() { return durationTicks > 0; }

    @Override
    protected @NotNull Skin getWorn(Player player) { return BodyService.getSkin(player); }

    @Override
    protected void temper(MortalGuItem gu, ServerPlayer player) {
        Amplification amplification = canBeAmplified() ? AmplifyEffectGuItem.bill(player, gu) : Amplification.NONE;
        BodyService.wearSkin(player, getGrade(), durationTicks * amplification.factor());
        TimedEffects.markSkinAmplified(player, amplification);
        SkinService.tickSkin(player);
    }
}
