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
 * 皮蛊行为：玉皮蛊[Jade Skin Gu]、白玉蛊[White Jade Gu]限时，铜皮蛊[Bronze Skin Gu]、铁皮蛊[Iron Skin Gu]永久。
 * <ul>
 *   <li>哪种皮、玉皮多久由注册定；⚠ 限时皮须正时长、永久皮不得有，构造时校验。</li>
 *   <li>经 {@link BodyService#wearSkin} 写皮后立即投影，HUD 不等心跳即显示玉皮；减伤、计时、闪光都挂在这次写入。</li>
 *   <li>功倍蛊[Amplify Effect Gu]倍增限时皮时长，永久皮不增幅不计费；{@code TimedEffects} 凭此找借出玉皮的蛊。</li>
 * </ul>
 *
 * @author Alex
 * @version 1.0.0
 * @see SkinService
 * @since 1.0.0
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
