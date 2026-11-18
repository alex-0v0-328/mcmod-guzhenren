package net.alex.guzhenren.item.gu.mortal.behavior;

import java.util.HashMap;
import java.util.Map;
import net.alex.guzhenren.Guzhenren;
import net.alex.guzhenren.effect.timed.WatchGuEffect;
import net.alex.guzhenren.gameplay.body.BodyService;
import net.alex.guzhenren.gameplay.body.ShieldDownEvent;
import net.alex.guzhenren.gameplay.body.ShieldLayer;
import net.alex.guzhenren.gameplay.body.ShieldRaisedEvent;
import net.alex.guzhenren.gameplay.body.ShieldService;
import net.alex.guzhenren.gameplay.body.ShieldType;
import net.alex.guzhenren.gameplay.body.Skin;
import net.alex.guzhenren.gameplay.body.SkinShedEvent;
import net.alex.guzhenren.gameplay.body.SkinWornEvent;
import net.alex.guzhenren.gameplay.path.PathMarks;
import net.alex.guzhenren.gameplay.path.GuPath;
import net.alex.guzhenren.gameplay.path.PathEffect;
import net.alex.guzhenren.gameplay.path.PathEffectEndedEvent;
import net.alex.guzhenren.gameplay.path.PathEffectService;
import net.alex.guzhenren.gameplay.path.PathEffectType;
import net.alex.guzhenren.gameplay.path.timed.TimedBuff;
import net.alex.guzhenren.item.gu.MortalGuItem;
import net.alex.guzhenren.item.gu.mortal.path.rule.AmplifyEffectGuItem.Amplification;
import net.alex.guzhenren.item.gu.mortal.path.water.ShieldBehavior;
import net.alex.guzhenren.registry.effect.ModPathEffectTypes;
import net.alex.guzhenren.registry.item.ModItems;
import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Unit;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * 限时凡蛊（兽力/冲撞/全力以赴/力气增益、限时玉皮、天蓬护盾、生机叶、更蛊[Watch Gu]时间流速）的身体流派效果管理与同步工
 * 具。
 * <ul>
 *   <li>所有限时效果到期后自动结束并消散其携带的道痕[Path Marks]点数，状态数值不按道痕数量缩放。</li>
 *   <li>付与状态的蛊虫通过扫描 {@code ModItems} 定位，并由其 {@code GuProperties} 提供载体、流派与道痕点数，效果来源取
 *       该蛊物品 ID；限时增益、生机叶与更蛊的 {@link PathEffect} 即状态真值，原版药水效果仅作图标、倒计时与等级投影
 *       （{@code /effect clear} 只能让图标暂隐至下一轮心跳，饮用牛奶亦无法清除），因此催动门槛与玩法规则（攻击、负重、
 *       时间流速）统一查询 {@link #findBuff} 等本类入口。</li>
 *   <li>限时玉皮与天蓬护盾的真值分别保存在 {@code BodyData} 与 {@code ShieldData} 中，本类监听其穿脱/升落事件同步流派效
 *       果，并在流派效果被移除或自然到期时反向清除对应皮肤或护盾。</li>
 *   <li>效果运行期间再次催动同一只蛊仅刷新或累加结束时间、不重复叠加道痕点数（唯独更蛊每次再催会叠加一层道痕点数直至层
 *       数上限）；更高等级的同种增益（如高转冲撞蛊）会直接替换旧效果及其道痕点数；若当次催动由功倍蛊[Amplify Effect Gu]
 *       增幅，{@link #markAmplified} 会将功倍蛊的律道点数补齐到被增幅效果上（重复增幅只补差额、不叠加超出单份功倍蛊点
 *       数）。</li>
 *   <li>⚠ {@code grant*} 系列方法仅写入 {@link PathEffectService}，将效果立即投影为原版药水图标等表现由调用方负
 *       责。</li>
 *   <li>⚠ 当限时皮或天蓬护盾效果因非替换原因（{@code Reason.REPLACED} 以外）结束时，{@link #onEnded} 仅在玩家当前皮肤或
 *       护盾仍归属该效果时才执行清除，避免触发事件死循环或误清已切换的新状态。</li>
 * </ul>
 */

@EventBusSubscriber(modid = Guzhenren.MOD_ID)
public final class TimedEffects {

    private TimedEffects() {}

    //region buffs [增益]
    public static @Nullable PathEffect<TimedBuff> findBuff(@NotNull Player player,
                                                           @NotNull Holder<MobEffect> effect) {
        PathEffect<TimedBuff> found = null;
        for (PathEffect<?> candidate : PathEffectService.getEffects(player)) {
            if (candidate.type() != ModPathEffectTypes.BUFF) continue;

            @SuppressWarnings("unchecked")
            PathEffect<TimedBuff> buff = (PathEffect<TimedBuff>) candidate;
            if (buff.payload().is(effect) && (found == null
                    || buff.payload().amplifier() > found.payload().amplifier())) found = buff;
        }
        return found;
    }

    public static boolean hasBuff(@NotNull Player player, @NotNull Holder<MobEffect> effect) {
        return findBuff(player, effect) != null;
    }

    public static int getBuffTicksLeft(@NotNull Player player, @NotNull Holder<MobEffect> effect) {
        PathEffect<TimedBuff> buff = findBuff(player, effect);
        return buff == null ? 0 : (int) Math.min(Integer.MAX_VALUE,
                buff.getTicksLeft(player.level().getGameTime()));
    }

    public static void grantBuff(@NotNull ServerPlayer player, @NotNull MortalGuItem gu,
                                 @NotNull Holder<MobEffect> effect, int amplifier, long durationTicks,
                                 @NotNull Amplification amplification) {
        ResourceLocation source = gu.getEffectSource();
        TimedBuff payload = TimedBuff.of(effect, amplifier);
        long endTick = player.level().getGameTime() + durationTicks;
        PathEffect<TimedBuff> own = PathEffectService.find(player, ModPathEffectTypes.BUFF, source);
        if (own != null && own.payload().equals(payload)) {
            PathEffectService.update(player, ModPathEffectTypes.BUFF, source, buff -> buff.withEndTick(endTick));
        } else {
            PathEffectService.remove(player, other -> other.type() == ModPathEffectTypes.BUFF
                    && other.payload() instanceof TimedBuff buff && buff.effect().equals(payload.effect()));
            PathEffectService.apply(player, gu.createEffect(ModPathEffectTypes.BUFF, endTick, payload));
        }
        markAmplified(player, ModPathEffectTypes.BUFF, source, amplification);
    }
    //endregion

    //region leaf and watch [生机叶与更蛊]
    public static boolean hasLeaf(@NotNull Player player) {
        return PathEffectService.getEffects(player).stream().anyMatch(
                effect -> effect.type() == ModPathEffectTypes.VITALITY_LEAF);
    }

    public static void grantLeaf(@NotNull ServerPlayer player, @NotNull MortalGuItem gu, long durationTicks,
                                 @NotNull Amplification amplification) {
        ResourceLocation source = gu.getEffectSource();
        long endTick = player.level().getGameTime() + durationTicks;
        PathEffectService.apply(player, gu.createEffect(ModPathEffectTypes.VITALITY_LEAF, endTick, Unit.INSTANCE));
        markAmplified(player, ModPathEffectTypes.VITALITY_LEAF, source, amplification);
    }

    public static void grantWatch(@NotNull ServerPlayer player, @NotNull MortalGuItem gu,
                                  @NotNull Holder<MobEffect> form, long durationTicks,
                                  @NotNull Amplification amplification) {
        ResourceLocation source = gu.getEffectSource();
        WatchGuEffect rate = (WatchGuEffect) form.value();
        long endTick = player.level().getGameTime() + durationTicks;
        PathEffect<ResourceLocation> own = PathEffectService.find(player, ModPathEffectTypes.WATCH_RATE, source);
        if (own == null) {
            PathEffectService.apply(player, gu.createEffect(
                    ModPathEffectTypes.WATCH_RATE, endTick, form.unwrapKey().orElseThrow().location()));
        } else {
            boolean stacks = own.layers() < rate.getNextAmplifier(own.layers() - 1) + 1;
            Map<GuPath, PathMarks> layer = gu.getGuProperties().getEffectPaths();
            PathEffectService.update(player, ModPathEffectTypes.WATCH_RATE, source, effect -> stacks
                    ? effect.addLayer(layer).withEndTick(endTick)
                    : effect.withEndTick(Math.max(effect.endTick(), endTick)));
        }
        markAmplified(player, ModPathEffectTypes.WATCH_RATE, source, amplification);
    }
    //endregion

    //region amplification [增幅]
    public static <P> void markAmplified(@NotNull ServerPlayer player, @NotNull PathEffectType<P> type,
                                         @NotNull ResourceLocation source, @NotNull Amplification amplification) {
        if (amplification.gu() == null) return;

        Map<GuPath, PathMarks> wanted = amplification.gu().getGuProperties().getEffectPaths();
        PathEffectService.update(player, type, source, effect -> {
            Map<GuPath, PathMarks> missing = new HashMap<>();
            wanted.forEach((path, marks) -> missing.put(path, marks.minus(effect.getMarks(path))));
            return effect.addMarks(missing);
        });
    }
    //endregion

    //region jade skin [玉皮]
    @SubscribeEvent
    public static void onSkinWorn(SkinWornEvent event) { syncSkin(event.getPlayer()); }

    @SubscribeEvent
    public static void onSkinShed(SkinShedEvent event) { syncSkin(event.getPlayer()); }

    public static void markSkinAmplified(@NotNull ServerPlayer player, @NotNull Amplification amplification) {
        PathEffect<Skin> worn = PathEffectService.find(player, ModPathEffectTypes.TIMED_SKIN);
        if (worn != null) markAmplified(player, ModPathEffectTypes.TIMED_SKIN, worn.source(), amplification);
    }

    private static void syncSkin(ServerPlayer player) {
        Skin skin = BodyService.getSkin(player);
        MortalGuItem gu = skin.isTimed()
                ? ModItems.findGuWith(SkinBehavior.class, behavior -> behavior.getGrade() == skin) : null;
        if (gu == null) {
            PathEffectService.removeType(player, ModPathEffectTypes.TIMED_SKIN);
            return;
        }
        long endTick = player.level().getGameTime() + BodyService.getSkinTicksLeft(player);
        PathEffect<Skin> worn = PathEffectService.find(player, ModPathEffectTypes.TIMED_SKIN);
        if (worn != null && worn.payload() == skin) {
            if (worn.endTick() != endTick) {
                PathEffectService.update(player, ModPathEffectTypes.TIMED_SKIN, worn.source(),
                        effect -> effect.withEndTick(endTick));
            }
            return;
        }
        PathEffectService.apply(player, gu.createEffect(ModPathEffectTypes.TIMED_SKIN, endTick, skin));
    }
    //endregion

    //region mugwort shield [天蓬护盾]
    @SubscribeEvent
    public static void onShieldRaised(ShieldRaisedEvent event) {
        if (event.getType() != ShieldType.HEAVENLY_MUGWORT) return;
        ServerPlayer player = event.getPlayer();
        MortalGuItem gu = findShieldGu(ShieldType.HEAVENLY_MUGWORT);
        ShieldLayer layer = ShieldService.find(player, ShieldType.HEAVENLY_MUGWORT);
        if (gu == null || layer == null) return;

        ResourceLocation source = gu.getEffectSource();
        long endTick = layer.endTick();
        if (PathEffectService.find(player, ModPathEffectTypes.MUGWORT_SHIELD, source) != null) {
            PathEffectService.update(player, ModPathEffectTypes.MUGWORT_SHIELD, source,
                    effect -> effect.withEndTick(endTick));
        } else {
            PathEffectService.apply(player, gu.createEffect(ModPathEffectTypes.MUGWORT_SHIELD, endTick, Unit.INSTANCE));
        }
    }

    @SubscribeEvent
    public static void onShieldDown(ShieldDownEvent event) {
        if (event.getType() != ShieldType.HEAVENLY_MUGWORT) return;
        PathEffectService.removeType(event.getPlayer(), ModPathEffectTypes.MUGWORT_SHIELD);
    }

    public static void markShieldAmplified(@NotNull ServerPlayer player, @NotNull ShieldType type,
                                           @NotNull Amplification amplification) {
        if (type != ShieldType.HEAVENLY_MUGWORT) return;
        MortalGuItem gu = findShieldGu(type);
        if (gu != null) markAmplified(player, ModPathEffectTypes.MUGWORT_SHIELD, gu.getEffectSource(), amplification);
    }

    private static @Nullable MortalGuItem findShieldGu(ShieldType type) {
        return ModItems.findGuWith(ShieldBehavior.class, behavior -> behavior.type() == type);
    }
    //endregion

    @SubscribeEvent
    public static void onEnded(PathEffectEndedEvent event) {
        if (event.getReason() == PathEffectEndedEvent.Reason.REPLACED) return;

        ServerPlayer player = event.getPlayer();
        PathEffect<?> effect = event.getEffect();
        if (effect.type() == ModPathEffectTypes.TIMED_SKIN && effect.payload() == BodyService.getSkin(player)) {
            BodyService.shedSkin(player);
        } else if (effect.type() == ModPathEffectTypes.MUGWORT_SHIELD) {
            ShieldService.dismiss(player, ShieldType.HEAVENLY_MUGWORT);
        }
    }
}
