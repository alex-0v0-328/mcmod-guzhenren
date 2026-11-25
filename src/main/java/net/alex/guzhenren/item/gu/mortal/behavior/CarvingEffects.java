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
 * 炼体蛊（永久皮、筋、骨）身体流派效果与 {@link BodyService} 档位之间的双向同步订阅类。
 * <ul>
 *   <li>流派效果作为 {@link BodyService} 所管炼体档位的账面投影，监听部位写档事件自动跟进，使写档的蛊虫或命令无需直接操
 *       作 {@link PathEffectService}；同时当效果被显式移除时（{@link #onEnded}），反向将对应部位重置回天生档位，保证炼
 *       体档位必带对应道痕[Path Marks]（永久兽力与人力不经过本类，其效果直接作为真值由 {@code PathStrengthService} 写
 *       入）。</li>
 *   <li>付与某档位的蛊虫通过扫描 {@code ModItems} 中的 {@link TemperingBehavior} 定位，并由其 {@code GuProperties} 提供
 *       载体、流派与道痕点数，效果来源取该蛊物品 ID；炼体档位为永久效果且每个部位互斥，写入新档位时会一并替换旧效果与道
 *       痕点数。</li>
 *   <li>⚠ 同步逻辑先比对现有效果与当前部位档位，不一致时才写入，重复调用幂等无害；道痕仅随设置部位档位的那次事件写入，
 *       玩家登录时不补发，因此旧存档在引入流派效果前已有的裸档位不会凭空生出道痕。</li>
 *   <li>⚠ 限时玉皮不归本类管理：在永久铜皮/铁皮上覆盖限时玉皮时，本类只移除永久皮效果；若 {@link #onEnded} 触发时玩家
 *       当前部位已切为其他档位，绝不误清当前部位。</li>
 * </ul>
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
