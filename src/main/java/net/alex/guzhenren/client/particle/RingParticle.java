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
 * 速度感与力感反馈通用的激波环粒子。
 * <ul>
 *   <li>使用 Alex 手绘的 {@link #FRAME_COUNT} 帧贴图并按 sprite 列表逐帧播放，每次投射一枚在生成点由小到大绽放（L2 环贴
 *       图测试锁定帧数与数据生成的 sprite 列表）；环面垂直于生成速度方向（{@link #spawnDirection}），并在侧对镜头时
 *       自动偏转至少 {@link RingGeometry#MIN_OPENING_DEGREES}。</li>
 *   <li>{@code RingConeEmitter} 沿冲刺路径释放 {@code shockwave_ring} 拖尾，并在每次范围冲击时从锥尖释放一枚
 *       {@code area_impact_ring}；{@code SecondStageRingEmitter} 在每次进入二段飞行[Second-Stage Flight]时释放一枚
 *       {@code sonic_boom_ring}（音爆环），并在维持高速巡航时在身前形成 {@code barrier_ring}（音障环）。</li>
 *   <li>各帧世界尺寸由画布宽与对应类型最大跨度算出（{@link RingGeometry#scaleForWidth(int, float)}）：冲刺拖尾采用
 *       {@link RingGeometry#LARGEST_RING_SPAN}，音爆环采用更粗像素的 {@link #SONIC_BOOM_SPAN}；每 tick 向下一帧插值
 *       （{@link #nextQuadSize}），使绽放过程平滑不跳格。</li>
 *   <li>新增环类型时只需在 {@code ModParticles} 注册并在数据生成 provider 列出帧图，再映射至
 *       {@link #createDashTrail(SpriteSet)}、{@link #createSonicBoom(SpriteSet)}、
 *       {@link #createBarrierRing(SpriteSet)} 或 {@link #createAreaImpactRing(SpriteSet)} 工厂之一。</li>
 *   <li>冲刺拖尾环（{@link #dashTrailRing}）在冲刺者本人第一人称视角下跳过渲染，仅面向第三人称与旁观者展示；
 *       {@link #noteLocalDash(int)} 开启 {@link #DASH_SELF_HIDE_TICKS} 隐藏窗口（连发总时长加一环含停留的寿命），
 *       {@code ownDashHiddenNow} 同时校验窗口上下界以防切换世界重置客户端 {@code tickCount}。</li>
 *   <li>⚠ {@link #RING_LINGER_TICKS} 让环在末帧停留片刻后再消失，否则肉眼难以看清完整绽放；离场方式由 {@link Exit}
 *       决定：冲刺环 {@link Exit#HOLD} 保持全亮，音爆环 {@link Exit#FADE} 在停留期渐隐。</li>
 *   <li>音障环采用 {@link Exit#SPREAD}：到达最大帧后的 {@link #SPREAD_LINGER_TICKS} tick 内（从绽放到消失共半秒）继续
 *       扩大至该帧的 {@link #SPREAD_GROWTH} 倍，前 {@link #SPREAD_FADE_START} 进度保持全亮、其后淡出，呈现留在空中的气
 *       浪消散效果。</li>
 *   <li>范围冲击环采用 {@link Exit#SWEEP}：仅在绽放期的前 {@link RingConeEmitter#AREA_IMPACT_TRAVEL_TICKS} tick 内随速
 *       度移动并扩张至 {@link #AREA_IMPACT_SPAN}，恰好停在冲击锥末端，随后原地每 tick 增长一帧步长（
 *       {@link RingGeometry#getSweepSize}）并在 {@link #AREA_IMPACT_LINGER_TICKS} 加 1 tick 内淡出；其余类型环则终生漂
 *       移。</li>
 *   <li>淡出与扩散均结合 partial tick 读取退场进度（{@link RingGeometry#getExitProgress}），保证帧间平滑过渡。</li>
 *   <li>⚠ {@link #tick()} 读取自增后的 {@code age}，使末帧恰好应用一次且绝不越界，若两侧混用 off-by-one 索引会直接崩
 *       溃。</li>
 *   <li>⚠ {@code applyFrame} 分母固定取 {@code FRAME_COUNT - 1} 而非 {@code lifetime - 1}：停留时间使寿命与动画帧数脱
 *       钩，只有按帧数分母才能精确对齐第 0 到 n-1 帧。</li>
 *   <li>⚠ 必须经 {@link #ADDITIVE_GLOW}（叠加混合、双面、全亮）渲染：单像素白描边在常规 Alpha 混合粒子图集中几乎不可
 *       见。</li>
 *   <li>1.21.1 的 {@link ParticleRenderType} 没有 {@code end()} 回调，因此不能向全局泄漏状态：混合模式与深度掩码由原
 *       版各 {@code begin} 重置，本类不修改背面剔除且图集过滤取默认；暂绑已弃用的
 *       {@code TextureAtlas.LOCATION_PARTICLES}，因为原版粒子同样绑定它且尚无替代。</li>
 *   <li>{@link #render} 提交两个反绕序四边形实现双面可见而无需修改全局剔除状态；{@code QUADS} 每 4 顶点成一组，反面
 *       UV 依然准确锚定在各自角点。</li>
 *   <li>唯一可变静态字段 {@code localDashHiddenUntilTick} 保存本地玩家隐藏自身冲刺环的 tick 截止戳；客户端仅存在一个
 *       本地玩家且窗口上下界受控，旧实体留下的时间戳不会误隐藏新环。</li>
 * </ul>
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
