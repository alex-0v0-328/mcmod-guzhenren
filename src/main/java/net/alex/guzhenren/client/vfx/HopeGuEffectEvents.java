package net.alex.guzhenren.client.vfx;

import com.lowdragmc.photon.client.fx.EntityEffectExecutor;
import com.lowdragmc.photon.client.fx.FXRuntime;
import java.util.Map;
import java.util.WeakHashMap;
import net.alex.guzhenren.Guzhenren;
import net.alex.guzhenren.client.hud.AwakeningHud;
import net.alex.guzhenren.client.hud.AwakeningPlaque;
import net.alex.guzhenren.client.renderer.AwakeningGuRenderer;
import net.alex.guzhenren.client.renderer.AwakeningPlaqueRenderer;
import net.alex.guzhenren.entity.gu.HopeGuEntity;
import net.alex.guzhenren.gameplay.aperture.awakening.AwakeningRitual;
import net.alex.guzhenren.gameplay.aperture.awakening.AwakeningRitualService;
import net.alex.guzhenren.gameplay.aperture.awakening.AwakeningTimeline;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.tick.EntityTickEvent;

/**
 * 希望蛊[Hope Gu]像素粒子（{@link HopeGuParticles}）的客户端触发。
 *
 * <ul>
 *   <li>野生希望蛊进入客户端世界即挂上核心与拖尾，跟到实体移除；核心随实体立即消失，拖尾放完余粒。实体
 *       {@link HopeGuEntity#isFading} 时两者停下，原地放消散效果。</li>
 *   <li>开窍[Awakening]仪式按整 tick 放：燃烬换帧放余烬，闪白起点放火星爆散，都在蛊的位置；牌匾每展开一档在上下两边
 *       各放一粒火星，每碎一行在两块中心各放一粒。</li>
 *   <li>牌匾火星：本人第一人称交给 {@link AwakeningHud} 画在屏幕牌匾上；否则在头顶牌匾对应像素处放世界粒子，
 *       看不见那块牌匾就不放。</li>
 *   <li>⚠ 跟着玩家刻走，每名玩家记上次处理的仪式 tick：{@code /tick freeze} 下玩家照刻、对时包可让游戏时间回退，同一
 *       tick 只放一次。</li>
 * </ul>
 *
 * @author Alex
 * @version 1.0.0
 * @see HopeGuParticles
 * @since 1.0.0
 */

@EventBusSubscriber(modid = Guzhenren.MOD_ID, value = Dist.CLIENT)
public final class HopeGuEffectEvents {

    private HopeGuEffectEvents() {}

    private static final Map<HopeGuEntity, Glow> GLOWS = new WeakHashMap<>();
    private static final Map<Player, Integer> RITUAL_TICKS = new WeakHashMap<>();

    @SubscribeEvent
    public static void onEntityJoin(EntityJoinLevelEvent event) {
        if (!event.getLevel().isClientSide() || !(event.getEntity() instanceof HopeGuEntity hope)) return;
        if (hope.isFading()) return;
        GLOWS.put(hope, new Glow(HopeGuParticles.follow(HopeGuParticles.core(), hope, true),
                HopeGuParticles.follow(HopeGuParticles.trail(), hope, false)));
    }

    @SubscribeEvent
    public static void onEntityTick(EntityTickEvent.Post event) {
        Entity entity = event.getEntity();
        if (!entity.level().isClientSide()) return;
        if (entity instanceof HopeGuEntity hope) {
            tickHopeGu(hope);
        } else if (entity instanceof Player player) {
            tickRitual(player);
        }
    }

    private static void tickHopeGu(HopeGuEntity hope) {
        if (!hope.isFading()) return;
        Glow glow = GLOWS.remove(hope);
        if (glow == null) return;

        stop(glow.core(), true);
        stop(glow.trail(), false);
        HopeGuParticles.play(HopeGuParticles.fade(), hope.level(),
                hope.position().add(0.0, hope.getBbHeight() / 2.0, 0.0));
    }

    private static void tickRitual(Player player) {
        AwakeningRitual ritual = AwakeningRitualService.get(player);
        if (ritual == null) {
            RITUAL_TICKS.remove(player);
            return;
        }

        Level level = player.level();
        int tick = (int) (level.getGameTime() - ritual.startTick());
        Integer last = RITUAL_TICKS.put(player, tick);
        if (last != null && last == tick) return;
        if (AwakeningTimeline.isBurnoutStep(tick)) {
            HopeGuParticles.play(HopeGuParticles.embers(), level,
                    AwakeningGuRenderer.getGuPosition(player, ritual, tick, 1.0F));
        } else if (tick == AwakeningTimeline.FLASH_START_TICKS) {
            HopeGuParticles.play(HopeGuParticles.burst(), level,
                    AwakeningGuRenderer.getGuPosition(player, ritual, tick, 1.0F));
        }
        AwakeningPlaque plaque = AwakeningPlaque.of(ritual);
        if (plaque != null) emitPlaqueSparks(player, plaque.height(), tick);
    }

    private static void emitPlaqueSparks(Player player, int height, int tick) {
        int[] points = new int[4];
        int count = 0;
        int rows = AwakeningTimeline.getUnfoldedRows(tick, height);
        if (rows > AwakeningTimeline.getUnfoldedRows(tick - 1, height)) {
            int top = (height - rows) / 2;
            points[count++] = AwakeningPlaque.WIDTH / 2;
            points[count++] = top;
            points[count++] = AwakeningPlaque.WIDTH / 2;
            points[count++] = top + rows;
        }
        int shattered = AwakeningTimeline.getShatteredRows(tick);
        int block = AwakeningTimeline.SHATTER_BLOCK;
        if (shattered > AwakeningTimeline.getShatteredRows(tick - 1) && (shattered - 1) * block < height) {
            int bottom = height - (shattered - 1) * block;
            int middle = (Math.max(0, bottom - block) + bottom) / 2;
            points[count++] = block / 2;
            points[count++] = middle;
            points[count++] = AwakeningPlaque.WIDTH - block / 2;
            points[count++] = middle;
        }
        if (count == 0) return;

        Minecraft minecraft = Minecraft.getInstance();
        Camera camera = minecraft.gameRenderer.getMainCamera();
        long gameTime = player.level().getGameTime();
        if (player == camera.getEntity() && !camera.isDetached()) {
            for (int i = 0; i < count; i += 2) {
                AwakeningHud.INSTANCE.addSpark(points[i], points[i + 1], height, gameTime);
            }
            return;
        }
        if (!AwakeningPlaqueRenderer.isVisible(minecraft, player, AwakeningPlaqueRenderer.getAnchor(player, 1.0F),
                camera.getPosition())) {
            return;
        }
        for (int i = 0; i < count; i += 2) {
            HopeGuParticles.play(HopeGuParticles.spark(), player.level(),
                    AwakeningPlaqueRenderer.getPixelPosition(player, camera, points[i], points[i + 1], height));
        }
    }

    private static void stop(EntityEffectExecutor executor, boolean force) {
        FXRuntime runtime = executor.getRuntime();
        if (runtime != null) runtime.destroy(force);
    }

    private record Glow(EntityEffectExecutor core, EntityEffectExecutor trail) {}
}
