package net.alex.guzhenren.client.vfx;

import com.lowdragmc.photon.client.fx.EntityEffectExecutor;
import com.lowdragmc.photon.client.fx.FX;
import com.lowdragmc.photon.client.fx.FXRuntime;
import com.lowdragmc.photon.client.fx.IEffectExecutor;
import com.lowdragmc.photon.client.gameobject.emitter.data.EmissionSetting;
import com.lowdragmc.photon.client.gameobject.emitter.data.MaterialSetting;
import com.lowdragmc.photon.client.gameobject.emitter.data.material.TextureMaterial;
import com.lowdragmc.photon.client.gameobject.emitter.data.number.NumberFunction;
import com.lowdragmc.photon.client.gameobject.emitter.data.number.NumberFunction3;
import com.lowdragmc.photon.client.gameobject.emitter.data.number.curve.Curve;
import com.lowdragmc.photon.client.gameobject.emitter.data.number.curve.ECBCurves;
import com.lowdragmc.photon.client.gameobject.emitter.data.shape.Circle;
import com.lowdragmc.photon.client.gameobject.emitter.data.shape.Cone;
import com.lowdragmc.photon.client.gameobject.emitter.data.shape.Dot;
import com.lowdragmc.photon.client.gameobject.emitter.data.shape.ShapeArcMode;
import com.lowdragmc.photon.client.gameobject.emitter.data.shape.Sphere;
import com.lowdragmc.photon.client.gameobject.emitter.particle.ParticleConfig;
import com.lowdragmc.photon.client.gameobject.emitter.particle.ParticleEmitter;
import com.lowdragmc.photon.client.gameobject.particle.ParticleLifetime;
import net.alex.guzhenren.Guzhenren;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;
import org.joml.Vector2i;
import org.joml.Vector3f;

/**
 * 希望蛊[Hope Gu]的 Photon 像素粒子定义，代码里搭发射器，不带 {@code .fx} 文件。
 *
 * <ul>
 *   <li>每粒 {@link #PIXELS}×{@link #PIXELS} 像素帧：萤光核心与消散小帧是蛊的身体，世界宽 0.5 格（16 像素/格，同原版实体）；
 *       其余粒子 0.25 格（32 像素/格）。pixelArt 材质按贴图宽对齐纹素中心，贴图只有全透明与不透明，序列帧按整帧跳，
 *       Photon 默认全亮。</li>
 *   <li>萤光：核心 {@code hope_glow} 1 粒跟实体，4 帧×5 tick 循环；拖尾 {@code hope_glow_mote} 世界空间，每移动
 *       {@link #TRAIL_SPACING} 格留 1 粒（飞行速度 0.1 格/tick 下约 2 tick 一粒），16 tick 寿命，至多 8 粒；另每
 *       {@link #IDLE_INTERVAL_TICKS} tick 在核心下沿落 1 粒（每 tick 加速 0.002 格/tick），悬停时也有动静。</li>
 *   <li>消散：小帧（第 2 帧）停 4 tick，随后 4 粒尘四向均分散开，0.02 格/tick。</li>
 *   <li>余烬：3 粒在半径 0.2 格环上均分，0.015 格/tick 上升，每 tick 再加速 0.001 格/tick；火星爆散 12 粒，
 *       0.04 格/tick 球面向外；牌匾火星 1 粒原地，6 帧×3 tick。</li>
 *   <li>定义首次用到时才搭（Photon 已就绪），每次播放复制一份运行时；定点效果由 {@link PointExecutor} 放出，到时自灭。</li>
 *   <li>⚠ 跟随效果都开 allowMulti：Photon 按 {@code fxLocation} 给同一实体去重，代码搭的定义没有位置（都为 null），
 *       不开则拖尾被当成核心的重复而不放。</li>
 * </ul>
 *
 * @author Alex
 * @version 1.0.0
 * @see HopeGuEffectEvents
 * @since 1.0.0
 */

public final class HopeGuParticles {

    private HopeGuParticles() {}

    private static final int PIXELS = 8;
    private static final float HALF_SIZE = 0.125F;
    private static final float BODY_HALF_SIZE = 0.25F;
    private static final float FRAME_EDGE = 1.0E-3F;
    private static final int EFFECT_TICKS = 20;
    private static final int CORE_CYCLE_TICKS = 20;
    private static final int MOTE_TICKS = 16;
    private static final int SPARK_TICKS = 18;
    private static final int SMALL_CORE_FRAME = 2;
    private static final int SMALL_CORE_TICKS = 4;
    private static final int TRAIL_MOTES = 8;
    private static final float TRAIL_SPACING = 0.2F;
    private static final int IDLE_INTERVAL_TICKS = 10;
    private static final int IDLE_MOTES = 2;
    private static final float IDLE_FALL = 0.05F;
    private static final float SHAPE_SPEED = 0.05F;
    private static final float FADE_SPEED = 0.02F;
    private static final float EMBER_RADIUS = 0.2F;
    private static final float EMBER_SPEED = 0.015F;
    private static final float EMBER_LIFT = -0.025F;
    private static final int EMBERS = 3;
    private static final float BURST_SPEED = 0.04F;
    private static final int BURST_SPARKS = 12;
    private static final int FADE_MOTES = 4;
    private static FX core;
    private static FX trail;
    private static FX fade;
    private static FX embers;
    private static FX burst;
    private static FX spark;

    public static @NotNull FX core() {
        if (core == null) {
            ParticleEmitter emitter = emitter("hope_glow", 4, CORE_CYCLE_TICKS);
            body(emitter);
            emitter.config.setLifetimeMode(ParticleLifetime.Mode.Loop);
            emitter.config.setSimulationSpace(ParticleConfig.Space.Local);
            emitter.config.setLooping(true);
            emitter.config.setMaxParticles(1);
            burst(emitter, 0, 1);
            core = fx(emitter);
        }
        return core;
    }

    public static @NotNull FX trail() {
        if (trail == null) {
            ParticleEmitter emitter = emitter("hope_glow_mote", 4, MOTE_TICKS);
            emitter.config.setLooping(true);
            emitter.config.setMaxParticles(TRAIL_MOTES);
            emitter.config.emission.setDistanceRate(NumberFunction.constant(TRAIL_SPACING));
            ParticleEmitter idle = emitter("hope_glow_mote", 4, MOTE_TICKS);
            idle.config.setLooping(true);
            idle.config.setMaxParticles(IDLE_MOTES);
            idle.config.emission.setEmissionRate(NumberFunction.constant(1.0F / IDLE_INTERVAL_TICKS));
            idle.config.shape.setPosition(new NumberFunction3(0.0F, -(BODY_HALF_SIZE + HALF_SIZE), 0.0F));
            idle.config.physics.setEnable(true);
            idle.config.physics.setHasCollision(false);
            idle.config.physics.setGravity(NumberFunction.constant(IDLE_FALL));
            trail = fx(emitter, idle);
        }
        return trail;
    }

    public static @NotNull FX fade() {
        if (fade == null) {
            ParticleEmitter small = emitter("hope_glow", 4, SMALL_CORE_TICKS);
            body(small);
            small.config.uvAnimation.setStartFrame(NumberFunction.constant(SMALL_CORE_FRAME));
            small.config.uvAnimation.setFrameOverTime(NumberFunction.constant(0));
            burst(small, 0, 1);
            ParticleEmitter motes = emitter("hope_glow_mote", 4, MOTE_TICKS);
            Circle ring = new Circle();
            ring.setRadius(0.01F);
            ring.setRadiusThickness(0.0F);
            ring.getShapeArc().setArcMode(ShapeArcMode.BurstSpread);
            motes.config.shape.setShape(ring);
            motes.config.setStartSpeed(NumberFunction.constant(FADE_SPEED / SHAPE_SPEED));
            burst(motes, SMALL_CORE_TICKS, FADE_MOTES);
            fade = fx(small, motes);
        }
        return fade;
    }

    public static @NotNull FX embers() {
        if (embers == null) {
            ParticleEmitter emitter = emitter("hope_burnout_ember", 4, MOTE_TICKS);
            Cone ring = new Cone();
            ring.setAngle(0.0F);
            ring.setRadius(EMBER_RADIUS);
            ring.setRadiusThickness(0.0F);
            ring.getShapeArc().setArcMode(ShapeArcMode.BurstSpread);
            emitter.config.shape.setShape(ring);
            emitter.config.setStartSpeed(NumberFunction.constant(EMBER_SPEED / SHAPE_SPEED));
            emitter.config.physics.setEnable(true);
            emitter.config.physics.setHasCollision(false);
            emitter.config.physics.setGravity(NumberFunction.constant(EMBER_LIFT));
            burst(emitter, 0, EMBERS);
            embers = fx(emitter);
        }
        return embers;
    }

    public static @NotNull FX burst() {
        if (burst == null) {
            ParticleEmitter emitter = emitter("hope_spark", 6, SPARK_TICKS);
            Sphere shell = new Sphere();
            shell.setRadius(0.05F);
            shell.setRadiusThickness(0.0F);
            emitter.config.shape.setShape(shell);
            emitter.config.setStartSpeed(NumberFunction.constant(BURST_SPEED / SHAPE_SPEED));
            burst(emitter, 0, BURST_SPARKS);
            burst = fx(emitter);
        }
        return burst;
    }

    public static @NotNull FX spark() {
        if (spark == null) {
            ParticleEmitter emitter = emitter("hope_spark", 6, SPARK_TICKS);
            burst(emitter, 0, 1);
            spark = fx(emitter);
        }
        return spark;
    }

    public static void play(@NotNull FX fx, @NotNull Level level, @NotNull Vec3 at) {
        FXRuntime runtime = fx.createRuntime();
        runtime.getRoot().updatePos(at.toVector3f());
        runtime.emit(new PointExecutor(level));
    }

    public static @NotNull EntityEffectExecutor follow(@NotNull FX fx, @NotNull Entity entity, boolean forcedDeath) {
        EntityEffectExecutor executor =
                new EntityEffectExecutor(fx, entity.level(), entity, EntityEffectExecutor.AutoRotate.NONE);
        executor.setOffset(new Vector3f(0.0F, entity.getBbHeight() / 2.0F - entity.getEyeHeight(), 0.0F));
        executor.setForcedDeath(forcedDeath);
        executor.setAllowMulti(true);
        executor.start();
        return executor;
    }

    private static ParticleEmitter emitter(String texture, int frames, int lifetime) {
        ParticleEmitter emitter = new ParticleEmitter();
        ParticleConfig config = emitter.config;
        TextureMaterial material = new TextureMaterial(Guzhenren.id("textures/particle/" + texture + ".png"));
        material.getPixelArt().setEnable(true);
        material.getPixelArt().bits = frames * PIXELS;
        config.renderer.getMaterials().clear();
        config.renderer.getMaterials().add(new MaterialSetting(material));
        config.setStartLifetime(NumberFunction.constant(lifetime));
        config.setStartSize(new NumberFunction3(HALF_SIZE, HALF_SIZE, HALF_SIZE));
        config.setStartSpeed(NumberFunction.constant(0));
        config.setLooping(false);
        config.setDuration(EFFECT_TICKS);
        config.setSimulationSpace(ParticleConfig.Space.World);
        config.emission.setEmissionRate(NumberFunction.constant(0));
        config.shape.setShape(new Dot());
        config.uvAnimation.setEnable(true);
        config.uvAnimation.setTiles(new Vector2i(frames, 1));
        config.uvAnimation.setFrameOverTime(new Curve(0.0F, 1.0F, 0.0F, 1.0F, "", "",
                new ECBCurves(0.0F, 0.0F, 1.0F / 3.0F, 1.0F / 3.0F, 2.0F / 3.0F, 2.0F / 3.0F, 1.0F, 1.0F)));
        config.uvAnimation.setCycle(frames - FRAME_EDGE);
        return emitter;
    }

    private static void body(ParticleEmitter emitter) {
        emitter.config.setStartSize(new NumberFunction3(BODY_HALF_SIZE, BODY_HALF_SIZE, BODY_HALF_SIZE));
    }

    private static void burst(ParticleEmitter emitter, int time, int count) {
        EmissionSetting.Burst burst = new EmissionSetting.Burst();
        burst.time = time;
        burst.setCount(NumberFunction.constant(count));
        emitter.config.emission.getBursts().add(burst);
    }

    private static FX fx(ParticleEmitter... emitters) {
        FX fx = new FX();
        for (ParticleEmitter emitter : emitters) fx.getFxData().objects().add(emitter);
        return fx;
    }

    private record PointExecutor(Level level) implements IEffectExecutor {

        @Override
        public Level getLevel() { return level; }
    }
}
