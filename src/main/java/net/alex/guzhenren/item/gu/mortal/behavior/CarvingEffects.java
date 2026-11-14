package net.alex.guzhenren.item.gu.mortal.behavior;

import net.alex.guzhenren.Guzhenren;
import net.alex.guzhenren.gameplay.body.BodyGrade;
import net.alex.guzhenren.gameplay.body.BodyService;
import net.alex.guzhenren.gameplay.body.BodyTemperedEvent;
import net.alex.guzhenren.gameplay.body.Bone;
import net.alex.guzhenren.gameplay.body.Skin;
import net.alex.guzhenren.gameplay.body.SkinShedEvent;
import net.alex.guzhenren.gameplay.body.SkinWornEvent;
import net.alex.guzhenren.gameplay.body.Tendon;
import net.alex.guzhenren.gameplay.path.PathEffect;
import net.alex.guzhenren.gameplay.path.PathEffectEndedEvent;
import net.alex.guzhenren.gameplay.path.PathEffectService;
import net.alex.guzhenren.gameplay.path.PathEffectType;
import net.alex.guzhenren.item.gu.MortalGuItem;
import net.alex.guzhenren.registry.effect.ModPathEffectTypes;
import net.alex.guzhenren.registry.item.ModItems;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;

/**
 * 炼体蛊的身体流派效果：皮、筋、骨。
 * <ul>
 *   <li>效果是 {@code BodyService} 所管档位的账面，随写档事件跟进，写档的蛊或命令无需知晓；档位反随效果
 *       （{@link #onEnded}）：效果被摘则部位回天生档，档位必带道痕[Path Marks]；兽力[Beast Strength]、人力不在此，
 *       其效果即存储，由 {@code PathStrengthService} 写。</li>
 *   <li>{@link PathEffectService} 上的事件订阅者；造档的蛊查 {@code ModItems}，其 {@code GuProperties} 给载体、
 *       道[Path]、丝，来源为蛊物品 id；档位永久，每部位一个独占效果，新档连旧效果与丝一并摘。</li>
 *   <li>⚠ 同步先比对效果与状态，有差才写，重复运行无害；⚠ 道痕只随设状态的那次写入而来，登录不补，效果出现前存的状态一
 *       直没有；⚠ 限时皮不归本类：永久皮上披玉皮只摘永久皮效果，部位已是别档时 {@code onEnded} 不动它。</li>
 * </ul>
 *
 * @author Alex
 * @version 1.0.0
 * @see PathEffectService
 * @since 1.0.0
 */

@EventBusSubscriber(modid = Guzhenren.MOD_ID)
public final class CarvingEffects {

    private CarvingEffects() {}

    @SubscribeEvent
    public static void onSkinWorn(SkinWornEvent event) { syncSkin(event.getPlayer()); }

    @SubscribeEvent
    public static void onSkinShed(SkinShedEvent event) { syncSkin(event.getPlayer()); }

    @SubscribeEvent
    public static void onTempered(BodyTemperedEvent event) {
        syncTendon(event.getPlayer());
        syncBone(event.getPlayer());
    }

    @SubscribeEvent
    public static void onEnded(PathEffectEndedEvent event) {
        if (event.getReason() != PathEffectEndedEvent.Reason.REMOVED) return;

        ServerPlayer player = event.getPlayer();
        Object payload = event.getEffect().payload();
        if (payload instanceof Skin skin) {
            if (BodyService.getSkin(player) == skin) BodyService.shedSkin(player);
        } else if (payload instanceof Tendon tendon) {
            if (BodyService.getTendon(player) == tendon) BodyService.temperTendon(player, Tendon.NATIVE);
        } else if (payload instanceof Bone bone) {
            if (BodyService.getBone(player) == bone) BodyService.temperBone(player, Bone.NATIVE);
        }
    }

    private static void syncSkin(ServerPlayer player) {
        Skin skin = BodyService.getSkin(player);
        syncGrade(player, ModPathEffectTypes.BODY_SKIN, skin, !skin.isTimed());
    }

    private static void syncTendon(ServerPlayer player) {
        syncGrade(player, ModPathEffectTypes.BODY_TENDON, BodyService.getTendon(player), true);
    }

    private static void syncBone(ServerPlayer player) {
        syncGrade(player, ModPathEffectTypes.BODY_BONE, BodyService.getBone(player), true);
    }

    private static <G extends Enum<G> & BodyGrade<G>> void syncGrade(
            ServerPlayer player, PathEffectType<G> type, G grade, boolean carries) {
        PathEffect<G> worn = PathEffectService.find(player, type);
        MortalGuItem gu = carries && !grade.isNative()
                ? ModItems.findGuWith(TemperingBehavior.class, behavior -> behavior.getGrade() == grade) : null;
        if (gu == null) {
            if (worn != null) PathEffectService.removeType(player, type);
            return;
        }
        if (worn != null && worn.payload() == grade) return;

        PathEffectService.apply(player, gu.createEffect(type, PathEffect.PERMANENT, grade));
    }
}
