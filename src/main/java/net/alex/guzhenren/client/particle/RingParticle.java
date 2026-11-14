package net.alex.guzhenren.client.particle;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.alex.guzhenren.Guzhenren;
import net.alex.guzhenren.particle.RingConeEmitter;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.client.particle.TextureSheetParticle;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;
import org.joml.Vector3f;

/**
 * 速度感与力感反馈用的通用激波环粒子。
 *
 * <ul>
 *   <li>Alex 手绘 {@link #FRAME_COUNT} 帧，依 sprite 列表播放，每次投一个，落点处由小到大绽放；L2 环贴图测试钉住帧数与
 *       数据生成的 sprite 列表；平面垂直于生成速度（{@link #spawnDirection}），仅侧对时向镜头倾斜
 *       {@link RingGeometry#MIN_OPENING_DEGREES}。</li>
 *   <li>{@code RingConeEmitter}：冲刺拖尾沿路径放 {@code shockwave_ring}，每次范围冲击从锥尖放一个
 *       {@code area_impact_ring}（范围冲击环）；{@code SecondStageRingEmitter}：每次进入二段飞行[Second-Stage Flight]开
 *       一个 {@code sonic_boom_ring}（音爆环），保持高速时前沿成 {@code barrier_ring}（音障环）。</li>
 *   <li>帧尺寸由画布宽与类型最大跨度算出（{@link RingGeometry#scaleForWidth(int, float)}）：拖尾用
 *       {@link RingGeometry#LARGEST_RING_SPAN}，音爆用 {@link #SONIC_BOOM_SPAN}，像素更粗；每 tick 向下一帧插值
 *       （{@link #nextQuadSize}），绽放不跳格。</li>
 *   <li>新类型：{@code ModParticles} 注册，数据生成 provider 列出环，再映射到 {@link #createDashTrail(SpriteSet)}、
 *       {@link #createSonicBoom(SpriteSet)}、{@link #createBarrierRing(SpriteSet)} 或
 *       {@link #createAreaImpactRing(SpriteSet)}。</li>
 *   <li>冲刺拖尾环（{@link #dashTrailRing}）在冲刺者本人第一人称下跳过，只给第三人称与旁观者看；
 *       {@link #noteLocalDash(int)} 启动 {@link #DASH_SELF_HIDE_TICKS}（连发时长加一环含停留的寿命）；
 *       {@code ownDashHiddenNow} 限定窗口两端，因换世界会重置客户端玩家的 {@code tickCount}。</li>
 *   <li>⚠ {@link #RING_LINGER_TICKS} 让环停在最后一帧再消失，否则读不出完整绽放；离场由 {@link Exit} 定：冲刺环
 *       {@link Exit#HOLD} 全亮，音爆 {@link Exit#FADE} 停留期淡出。</li>
 *   <li>音障环 {@link Exit#SPREAD}：最大帧后 {@link #SPREAD_LINGER_TICKS} tick 内（绽放到消失共半秒）扩到该帧的
 *       {@link #SPREAD_GROWTH} 倍，前 {@link #SPREAD_FADE_START} 进度全亮、其后淡出，二段飞行留下的环向外消散。</li>
 *   <li>范围冲击环 {@link Exit#SWEEP}：移动中绽放到 {@link #AREA_IMPACT_SPAN}，只在前
 *       {@link RingConeEmitter#AREA_IMPACT_TRAVEL_TICKS} tick（绽放期）移动，恰停在锥末端；随后原地每 tick 长一帧步长
 *       （{@link RingGeometry#getSweepSize}），{@link #AREA_IMPACT_LINGER_TICKS} 加 1 tick 内淡出；他环终生漂移。</li>
 *   <li>淡出与扩散带 partial tick 读退场进度（{@link RingGeometry#getExitProgress}），tick 间平滑。</li>
 *   <li>⚠ {@link #tick()} 读自增后的 age，最后一帧恰好应用一次、不越界；两侧混用差一崩溃。</li>
 *   <li>⚠ {@code applyFrame} 分母取 {@code FRAME_COUNT - 1} 而非 {@code lifetime - 1}：停留使寿命与帧数脱钩，只有帧数能
 *       精确落在第 0 到 n-1 帧。</li>
 *   <li>⚠ 经 {@link #ADDITIVE_GLOW} 渲染（叠加、双面、全亮）：单像素白描边在 alpha 混合图集上几乎消失。</li>
 *   <li>1.21.1 {@link ParticleRenderType} 无 {@code end()} 钩子，状态不可外漏：混合与深度掩码由原版各 {@code begin} 重
 *       设，不碰剔除，图集过滤取默认；绑已弃用的 {@code TextureAtlas.LOCATION_PARTICLES}，因原版同样绑它且无替代。</li>
 *   <li>{@link #render} 出两个反绕序四边形，双面可见又不碰全局剔除；{@code QUADS} 每 4 顶点一组，反面 UV 仍钉各自角
 *       点。</li>
 *   <li>唯一可变静态字段 {@code localDashHiddenUntilTick}：本地玩家隐藏自身冲刺环的 tick 戳；客户端只有一个本地玩家，窗
 *       口两端有界，旧实体留下的戳不隐藏任何东西。</li>
 * </ul>
 *
 * @author Alex
 * @version 1.0.0
 * @see net.alex.guzhenren.registry.particle.ModParticles
 * @see RingGeometry
 * @since 1.0.0
 */

public final class RingParticle extends TextureSheetParticle {

    enum Exit { HOLD, FADE, SPREAD, SWEEP }

    public static final ParticleRenderType ADDITIVE_GLOW = new ParticleRenderType() {
        @SuppressWarnings("deprecation")
        @Override
        public BufferBuilder begin(Tesselator tesselator, @NotNull TextureManager textureManager) {
            RenderSystem.enableBlend();
            RenderSystem.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE);
            RenderSystem.depthMask(false);
            RenderSystem.setShaderTexture(0, TextureAtlas.LOCATION_PARTICLES);
            return tesselator.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.PARTICLE);
        }

        @Override
        public String toString() { return Guzhenren.MOD_ID + ":additive_glow"; }
    };
    static final int FRAME_COUNT = 5;
    static final float SONIC_BOOM_SPAN = 4.0F;
    private static final int RING_LINGER_TICKS = 3;
    static final int SPREAD_LINGER_TICKS = 9;
    static final float SPREAD_GROWTH = 4.0F;
    static final float SPREAD_FADE_START = 0.5F;
    static final float AREA_IMPACT_SPAN = 3.5F;
    static final int AREA_IMPACT_LINGER_TICKS = 4;
    private static final int DASH_SELF_HIDE_TICKS = 20;
    private static final int DRIFTS_FOR_LIFE = Integer.MAX_VALUE;
    private static int localDashHiddenUntilTick = Integer.MIN_VALUE;
    private final SpriteSet sprites;
    private final boolean dashTrailRing;
    private final float largestSpan;
    private final Exit exit;
    private final int travelTicks;
    private final Vec3 spawnDirection;
    private final float bloomStep;
    private float nextQuadSize;

    private RingParticle(ClientLevel level, double x, double y, double z,
                         double xSpeed, double ySpeed, double zSpeed,
                         SpriteSet sprites, boolean dashTrailRing, float largestSpan, Exit exit, int lingerTicks,
                         int travelTicks) {
        super(level, x, y, z);
        this.sprites = sprites;
        this.dashTrailRing = dashTrailRing;
        this.largestSpan = largestSpan;
        this.exit = exit;
        this.travelTicks = travelTicks;
        this.spawnDirection = new Vec3(xSpeed, ySpeed, zSpeed);
        this.lifetime = FRAME_COUNT + lingerTicks;
        this.hasPhysics = false;
        this.xd = xSpeed;
        this.yd = ySpeed;
        this.zd = zSpeed;
        int firstWidth = sprites.get(0, FRAME_COUNT - 1).contents().width();
        int lastWidth = sprites.get(FRAME_COUNT - 1, FRAME_COUNT - 1).contents().width();
        this.bloomStep = (RingGeometry.scaleForWidth(lastWidth, largestSpan)
                - RingGeometry.scaleForWidth(firstWidth, largestSpan)) / (FRAME_COUNT - 1);
        applyFrame(0);
    }

    public static ParticleProvider<SimpleParticleType> createDashTrail(SpriteSet sprites) {
        return create(sprites, true, RingGeometry.LARGEST_RING_SPAN, Exit.HOLD, RING_LINGER_TICKS, DRIFTS_FOR_LIFE);
    }

    public static ParticleProvider<SimpleParticleType> createSonicBoom(SpriteSet sprites) {
        return create(sprites, false, SONIC_BOOM_SPAN, Exit.FADE, RING_LINGER_TICKS, DRIFTS_FOR_LIFE);
    }

    public static ParticleProvider<SimpleParticleType> createBarrierRing(SpriteSet sprites) {
        return create(sprites, false, RingGeometry.LARGEST_RING_SPAN, Exit.SPREAD, SPREAD_LINGER_TICKS,
                DRIFTS_FOR_LIFE);
    }

    public static ParticleProvider<SimpleParticleType> createAreaImpactRing(SpriteSet sprites) {
        return create(sprites, false, AREA_IMPACT_SPAN, Exit.SWEEP, AREA_IMPACT_LINGER_TICKS,
                RingConeEmitter.AREA_IMPACT_TRAVEL_TICKS);
    }

    private static ParticleProvider<SimpleParticleType> create(SpriteSet sprites, boolean dashTrailRing,
                                                               float largestSpan, Exit exit, int lingerTicks,
                                                               int travelTicks) {
        return (type, level, x, y, z, xSpeed, ySpeed, zSpeed) -> new RingParticle(level, x, y, z, xSpeed, ySpeed,
                zSpeed, sprites, dashTrailRing, largestSpan, exit, lingerTicks, travelTicks);
    }

    public static void noteLocalDash(int tickCount) {
        localDashHiddenUntilTick = tickCount + DASH_SELF_HIDE_TICKS;
    }

    @Override
    public void tick() {
        this.xo = this.x;
        this.yo = this.y;
        this.zo = this.z;
        this.age++;
        if (this.age >= this.lifetime) {
            this.remove();
            return;
        }
        applyFrame(Math.min(this.age, FRAME_COUNT - 1));
        if (this.age > this.travelTicks) return;
        this.x += this.xd;
        this.y += this.yd;
        this.z += this.zd;
    }

    private void applyFrame(int frame) {
        this.setSprite(this.sprites.get(frame, FRAME_COUNT - 1));
        this.quadSize = RingGeometry.scaleForWidth(this.sprite.contents().width(), this.largestSpan);
        this.nextQuadSize = frame + 1 < FRAME_COUNT
                ? RingGeometry.scaleForWidth(
                        this.sprites.get(frame + 1, FRAME_COUNT - 1).contents().width(), this.largestSpan)
                : this.quadSize;
    }

    @Override
    public float getQuadSize(float partialTick) {
        float size = Mth.lerp(partialTick, this.quadSize, this.nextQuadSize);
        if (this.exit == Exit.SWEEP) {
            return RingGeometry.getSweepSize(this.age + partialTick, FRAME_COUNT - 1, size, this.bloomStep);
        }
        return this.exit == Exit.SPREAD ? size * (1.0F + (SPREAD_GROWTH - 1.0F) * getExitProgress(partialTick)) : size;
    }

    private float getExitProgress(float partialTick) {
        return RingGeometry.getExitProgress(this.age + partialTick, FRAME_COUNT - 1, this.lifetime - FRAME_COUNT);
    }

    @Override
    public void render(@NotNull VertexConsumer buffer, @NotNull Camera camera, float partialTick) {
        if (this.dashTrailRing && isOwnDashHiddenNow()) return;
        float alpha = this.exit == Exit.HOLD ? this.alpha : RingGeometry.getFadeAlpha(getExitProgress(partialTick),
                this.exit == Exit.SPREAD ? SPREAD_FADE_START : 0.0F);
        float size = this.getQuadSize(partialTick);
        Vector3f look = camera.getLookVector();
        Vector3f[] corners = RingGeometry.getFacing(this.spawnDirection, new Vec3(look.x(), look.y(), look.z()), size);
        Vec3 cameraPosition = camera.getPosition();
        float dx = (float) (Mth.lerp(partialTick, this.xo, this.x) - cameraPosition.x());
        float dy = (float) (Mth.lerp(partialTick, this.yo, this.y) - cameraPosition.y());
        float dz = (float) (Mth.lerp(partialTick, this.zo, this.z) - cameraPosition.z());
        for (Vector3f corner : corners) {
            corner.add(dx, dy, dz);
        }
        emitQuad(buffer, alpha, corners[0], this.getU0(), this.getV0(), corners[1], this.getU0(), this.getV1(),
                corners[2], this.getU1(), this.getV1(), corners[3], this.getU1(), this.getV0());
        emitQuad(buffer, alpha, corners[0], this.getU0(), this.getV0(), corners[3], this.getU1(), this.getV0(),
                corners[2], this.getU1(), this.getV1(), corners[1], this.getU0(), this.getV1());
    }

    private void emitQuad(VertexConsumer buffer, float alpha, Vector3f a, float au, float av,
                          Vector3f b, float bu, float bv, Vector3f c, float cu, float cv,
                          Vector3f delta, float du, float dv) {
        emitVertex(buffer, alpha, a, au, av);
        emitVertex(buffer, alpha, b, bu, bv);
        emitVertex(buffer, alpha, c, cu, cv);
        emitVertex(buffer, alpha, delta, du, dv);
    }

    private static boolean isOwnDashHiddenNow() {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null
                || !minecraft.options.getCameraType().isFirstPerson()) return false;
        int remaining = localDashHiddenUntilTick - minecraft.player.tickCount;
        return remaining > 0 && remaining <= DASH_SELF_HIDE_TICKS;
    }

    private void emitVertex(VertexConsumer buffer, float alpha, Vector3f corner, float u, float value) {
        buffer.addVertex(corner.x(), corner.y(), corner.z())
                .setUv(u, value)
                .setColor(this.rCol, this.gCol, this.bCol, alpha)
                .setLight(LightTexture.FULL_BRIGHT);
    }

    @Override
    public @NotNull ParticleRenderType getRenderType() { return ADDITIVE_GLOW; }
}
