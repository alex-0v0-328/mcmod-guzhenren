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
 * 希望蛊[Hope Gu]像素粒子（{@link HopeGuParticles}）的客户端事件触发器。
 * <ul>
 *   <li>野生希望蛊实体进入客户端世界时立即挂载核心与拖尾粒子并跟随至实体移除（核心随实体即刻消失，拖尾放完余粒）；当
 *       实体进入 {@link HopeGuEntity#isFading} 状态时停止跟随并在原地播放消散特效。</li>
 *   <li>开窍[Awakening]仪式期间按整 tick 触发：燃烬每次换帧释放余烬粒子，闪白起点在蛊虫位置释放火星爆散；资质
 *       [Aptitude]牌匾每展开一档在上下边缘各释放一粒火星，每碎裂一行在左右两半中心各释放一粒火星。</li>
 *   <li>牌匾火星在本人第一人称下直接交由 {@link AwakeningHud} 绘制在屏幕牌匾上；其他视角则在头顶世界牌匾对应像素位置
 *       生成世界粒子（观察者不可见该牌匾时跳过）。</li>
 *   <li>⚠ 特效推进跟随玩家刻运行，并为每名玩家记录上次处理的仪式 tick：在 {@code /tick freeze} 下玩家仍继续运行或
 *       对时包回退游戏时间时，同一仪式 tick 保证只触发一次。</li>
 * </ul>
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
