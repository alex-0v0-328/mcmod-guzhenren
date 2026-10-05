package net.alex.guzhenren.client.particle;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.alex.guzhenren.Guzhenren;
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
 * The reusable shockwave ring [激波环] for speed- and force-feel feedback: Alex's {@link #FRAME_COUNT}
 * hand-drawn rings played in sprite-list order, one particle per drop (the L2 ring-texture test pins the
 * count against the datagen'd sprite lists). Both trails go through {@code RingConeEmitter} and bloom
 * small-to-large where they were planted: the dash trail rides {@code shockwave_ring} along the dash path,
 * the punch trail rides {@code impact_ring} along the punch ray. Each frame's world size derives from its own
 * canvas width ({@link RingGeometry#scaleForWidth(int)}), and the quad size lerps toward the next frame every
 * tick ({@link #nextQuadSize}), so the bloom reads smooth instead of stepping.
 *
 * <p>{@link Orientation} is chosen per particle type by the provider: {@link Orientation#GROUND} lays the
 * quad flat in the world XZ plane, never rotated by the camera; {@link Orientation#FACING_MOTION} builds the
 * ring plane perpendicular to the spawn velocity ({@link #spawnDirection}), tilting toward the camera by
 * {@link RingGeometry#MIN_OPENING_DEGREES} only when the plane would be edge-on. New callers register a
 * type in {@code ModParticles}, list the rings in the datagen provider, and map the type to
 * {@link #facingMotion(SpriteSet)} or {@link #dashTrail(SpriteSet)}.
 *
 * <p>A dash-trail ring ({@link #dashTrailRing}) is skipped by the dasher's own first-person camera: the
 * trail is for third-person and bystanders (Alex, 2026-09-20). {@link #noteLocalDash(int)} arms
 * {@link #DASH_SELF_HIDE_TICKS} -- the burst length plus one ring life including its linger -- and
 * {@code ownDashHiddenNow} bounds both ends of the window, because a world change resets the client
 * player's tickCount.
 *
 * <p>⚠ {@link #RING_LINGER_TICKS} lets a ring hold its last frame before despawning, or the completed
 * bloom never reads. ⚠ {@link #tick()} reads the age AFTER the increment, so the last frame is applied
 * exactly once and never overstepped; mixing the two sides is an off-by-one crash. ⚠ In
 * {@code applyFrame} the denominator is {@code FRAME_COUNT - 1}, NOT {@code lifetime - 1}: only the frame
 * count lands exactly on frames 0..n-1 once the linger decouples the lifetime from it.
 *
 * <p>⚠ It renders through {@link #ADDITIVE_GLOW} -- additive, two-sided, full bright -- because the
 * one-pixel white stroke all but disappears on an alpha-blended sheet. 1.21.1 has no {@code end()} hook on
 * {@link ParticleRenderType}, so nothing in it may leak to the next type: blend and depth mask are re-set by
 * every vanilla {@code begin}, culling is never touched (the quad is emitted two-sided instead), and the
 * atlas filter stays at the vanilla default. The deprecated {@code TextureAtlas.LOCATION_PARTICLES} id is
 * bound because vanilla's own particle types bind it and there is no replacement.
 *
 * <p>{@link #render} emits two quads with opposite winding (8 vertices), so the ring shows from both sides
 * without touching the global cull state; {@code QUADS} groups vertices four by four, and corner UVs stay
 * pinned to their corners on the reversed side. Its history (the 2026-09-19 sinking and invisible rings,
 * the first-dash crash) is in the wiki's 《激波环与粒子》.
 *
 * @author Alex
 * @version 1.0.0
 * @see net.alex.guzhenren.registry.particle.ModParticles
 * @see RingGeometry
 * @since 1.0.0
 */

public final class RingParticle extends TextureSheetParticle {

    public enum Orientation { GROUND, FACING_MOTION }

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
    private static final int RING_LINGER_TICKS = 3;
    private static final int DASH_SELF_HIDE_TICKS = 20;
    private static int localDashHiddenUntilTick = Integer.MIN_VALUE;
    private final SpriteSet sprites;
    private final Orientation orientation;
    private final boolean dashTrailRing;
    private final Vec3 spawnDirection;
    private float nextQuadSize;

    private RingParticle(ClientLevel level, double x, double y, double z,
                         double xSpeed, double ySpeed, double zSpeed,
                         SpriteSet sprites, Orientation orientation, boolean dashTrailRing,
                         int lingerTicks) {
        super(level, x, y, z);
        this.sprites = sprites;
        this.orientation = orientation;
        this.dashTrailRing = dashTrailRing;
        this.spawnDirection = new Vec3(xSpeed, ySpeed, zSpeed);
        this.lifetime = FRAME_COUNT + lingerTicks;
        this.hasPhysics = false;
        this.xd = xSpeed;
        this.yd = ySpeed;
        this.zd = zSpeed;
        applyFrame(0);
    }

    public static ParticleProvider<SimpleParticleType> facingMotion(SpriteSet sprites) {
        return (type, level, x, y, z, xSpeed, ySpeed, zSpeed) ->
                new RingParticle(level, x, y, z, xSpeed, ySpeed, zSpeed, sprites,
                        Orientation.FACING_MOTION, false, RING_LINGER_TICKS);
    }

    public static ParticleProvider<SimpleParticleType> dashTrail(SpriteSet sprites) {
        return (type, level, x, y, z, xSpeed, ySpeed, zSpeed) ->
                new RingParticle(level, x, y, z, xSpeed, ySpeed, zSpeed, sprites,
                        Orientation.FACING_MOTION, true, RING_LINGER_TICKS);
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
        this.x += this.xd;
        this.y += this.yd;
        this.z += this.zd;
    }

    private void applyFrame(int frame) {
        this.setSprite(this.sprites.get(frame, FRAME_COUNT - 1));
        this.quadSize = RingGeometry.scaleForWidth(this.sprite.contents().width());
        this.nextQuadSize = frame + 1 < FRAME_COUNT
                ? RingGeometry.scaleForWidth(
                        this.sprites.get(frame + 1, FRAME_COUNT - 1).contents().width())
                : this.quadSize;
    }

    @Override
    public float getQuadSize(float partialTick) {
        return Mth.lerp(partialTick, this.quadSize, this.nextQuadSize);
    }

    @Override
    public void render(@NotNull VertexConsumer buffer, @NotNull Camera camera, float partialTick) {
        if (this.dashTrailRing && ownDashHiddenNow()) return;
        float size = this.getQuadSize(partialTick);
        Vector3f look = camera.getLookVector();
        Vector3f[] corners = this.orientation == Orientation.FACING_MOTION
                ? RingGeometry.facing(this.spawnDirection,
                        new Vec3(look.x(), look.y(), look.z()), size)
                : RingGeometry.ground(size);
        Vec3 cameraPosition = camera.getPosition();
        float dx = (float) (Mth.lerp(partialTick, this.xo, this.x) - cameraPosition.x());
        float dy = (float) (Mth.lerp(partialTick, this.yo, this.y) - cameraPosition.y());
        float dz = (float) (Mth.lerp(partialTick, this.zo, this.z) - cameraPosition.z());
        for (Vector3f corner : corners) {
            corner.add(dx, dy, dz);
        }
        emitQuad(buffer, corners[0], this.getU0(), this.getV0(), corners[1], this.getU0(), this.getV1(),
                corners[2], this.getU1(), this.getV1(), corners[3], this.getU1(), this.getV0());
        emitQuad(buffer, corners[0], this.getU0(), this.getV0(), corners[3], this.getU1(), this.getV0(),
                corners[2], this.getU1(), this.getV1(), corners[1], this.getU0(), this.getV1());
    }

    private void emitQuad(VertexConsumer buffer, Vector3f a, float au, float av,
                          Vector3f b, float bu, float bv, Vector3f c, float cu, float cv,
                          Vector3f delta, float du, float dv) {
        emitVertex(buffer, a, au, av);
        emitVertex(buffer, b, bu, bv);
        emitVertex(buffer, c, cu, cv);
        emitVertex(buffer, delta, du, dv);
    }

    private static boolean ownDashHiddenNow() {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null
                || !minecraft.options.getCameraType().isFirstPerson()) return false;
        int remaining = localDashHiddenUntilTick - minecraft.player.tickCount;
        return remaining > 0 && remaining <= DASH_SELF_HIDE_TICKS;
    }

    private void emitVertex(VertexConsumer buffer, Vector3f corner, float u, float value) {
        buffer.addVertex(corner.x(), corner.y(), corner.z())
                .setUv(u, value)
                .setColor(this.rCol, this.gCol, this.bCol, this.alpha)
                .setLight(LightTexture.FULL_BRIGHT);
    }

    @Override
    public @NotNull ParticleRenderType getRenderType() { return ADDITIVE_GLOW; }
}
