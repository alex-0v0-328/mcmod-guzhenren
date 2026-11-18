package net.alex.guzhenren.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.alex.guzhenren.Guzhenren;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FastColor;
import org.jetbrains.annotations.NotNull;

/**
 * 叠加在玩家渲染器上的皮肤闪现渲染层。
 * <ul>
 *   <li>以略微膨胀的半透明单色模型副本包裹玩家以防止与皮肤发生深度冲突，随闪现 Alpha 逐渐淡出；采用纯白贴图配合顶点
 *       染色驱动颜色，仅在闪现期间渲染，日常穿戴皮肤不修改玩家自身模型。</li>
 *   <li>通过 {@code EntityRenderersEvent.AddLayers} 分别挂载到宽、窄两套玩家模型渲染器上；每帧同步父模型的骨骼姿态
 *       （与原版能量漩涡层一致）以及外层衣物可见性（帽子、外套、袖子、裤子），以免隐藏外层的玩家身上多出膨胀幻影。</li>
 * </ul>
 */

public final class SkinFlashLayer extends RenderLayer<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>> {

    private static final ResourceLocation TEXTURE = Guzhenren.id("textures/entity/skin_flash.png");
    private static final float INFLATE = 0.25F;
    private static final int TEXTURE_SIZE = 64;
    private final PlayerModel<AbstractClientPlayer> model;

    public SkinFlashLayer(RenderLayerParent<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>> parent,
                          boolean slim) {
        super(parent);
        this.model = new PlayerModel<>(LayerDefinition.create(PlayerModel.createMesh(new CubeDeformation(INFLATE),
                slim), TEXTURE_SIZE, TEXTURE_SIZE).bakeRoot(), slim);
    }

    @Override
    public void render(@NotNull PoseStack poseStack, @NotNull MultiBufferSource buffer, int packedLight,
                       @NotNull AbstractClientPlayer player, float limbSwing, float limbSwingAmount,
                       float partialTick, float ageInTicks, float netHeadYaw, float headPitch) {
        if (player.isInvisible()) return;

        SkinFlashes.Flash flash = SkinFlashes.get(player.getId(), player.level().getGameTime());
        if (flash == null) return;

        float alpha = flash.getAlpha(player.level().getGameTime(), partialTick);
        if (alpha <= 0.0F) return;

        PlayerModel<AbstractClientPlayer> parent = getParentModel();
        parent.copyPropertiesTo(model);
        model.hat.visible = parent.hat.visible;
        model.jacket.visible = parent.jacket.visible;
        model.leftSleeve.visible = parent.leftSleeve.visible;
        model.rightSleeve.visible = parent.rightSleeve.visible;
        model.leftPants.visible = parent.leftPants.visible;
        model.rightPants.visible = parent.rightPants.visible;
        model.prepareMobModel(player, limbSwing, limbSwingAmount, partialTick);
        model.setupAnim(player, limbSwing, limbSwingAmount, ageInTicks, netHeadYaw, headPitch);
        VertexConsumer consumer = buffer.getBuffer(RenderType.entityTranslucent(TEXTURE));
        int tint = flash.getTint();
        model.renderToBuffer(poseStack, consumer, packedLight, OverlayTexture.NO_OVERLAY,
                FastColor.ARGB32.color(Math.round(alpha * 255.0F), tint));
    }
}
