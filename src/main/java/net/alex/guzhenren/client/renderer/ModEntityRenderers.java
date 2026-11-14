package net.alex.guzhenren.client.renderer;

import net.alex.guzhenren.Guzhenren;
import net.alex.guzhenren.entity.beast.BearEntity;
import net.alex.guzhenren.entity.beast.CattleEntity;
import net.alex.guzhenren.entity.beast.ElephantEntity;
import net.alex.guzhenren.entity.beast.TigerEntity;
import net.alex.guzhenren.entity.beast.TortoiseEntity;
import net.alex.guzhenren.entity.beast.WildBoarEntity;
import net.alex.guzhenren.entity.gu.BoarGuEntity;
import net.alex.guzhenren.entity.gu.CrashGuEntity;
import net.alex.guzhenren.registry.entity.ModEntityTypes;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.NoopRenderer;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import software.bernie.geckolib.model.DefaultedEntityGeoModel;
import software.bernie.geckolib.model.GeoModel;

/**
 * 本模组注册的全部实体渲染器。
 *
 * <ul>
 *   <li>每族共用一个 GeckoLib 模型，每实体类型一张固定贴图；希望蛊[Hope Gu]与气波用 {@link NoopRenderer}（纯粒子）；月
 *       刃用 {@link MoonBladeRenderer} sprite；兽力虚影用 {@link BeastPhantomRenderer}。</li>
 *   <li>豕蛊[Boar Gu]的瓢虫几何按真实大小建模（约 0.19 × 0.28 格），{@link #BOAR_GU_SCALE} 画到 0.4 格碰撞箱宽，不改导
 *       出几何，保留原版死亡倾倒；其余族关闭，其死亡动画自会放倒身体。</li>
 *   <li>象按半尺寸建模，128 × 128 贴图每单位一纹素，{@link #ELEPHANT_SCALE} 放大一倍，约 3.75 格高。</li>
 *   <li>冲撞蛊[Crash Gu]贴图按转数：两只三转暗银（共用一文件），四转暗金，五转暗紫晶。</li>
 * </ul>
 *
 * @author Alex
 * @version 1.0.0
 * @since 1.0.0
 */

public final class ModEntityRenderers {

    private ModEntityRenderers() {}

    private static final float BOAR_GU_SCALE = 2.0F;
    private static final float ELEPHANT_SCALE = 2.0F;
    private static final GeoModel<BoarGuEntity> BOAR_GU_MODEL =
            new DefaultedEntityGeoModel<>(Guzhenren.id("boar_gu"), false);
    private static final GeoModel<CrashGuEntity> CRASH_GU_MODEL =
            new DefaultedEntityGeoModel<>(Guzhenren.id("rhinoceros_beetle"), false);
    private static final GeoModel<WildBoarEntity> WILD_BOAR_MODEL =
            new DefaultedEntityGeoModel<>(Guzhenren.id("wild_boar"), false);
    private static final GeoModel<BearEntity> BEAR_MODEL =
            new DefaultedEntityGeoModel<>(Guzhenren.id("bear"), false);
    private static final GeoModel<TigerEntity> TIGER_MODEL =
            new DefaultedEntityGeoModel<>(Guzhenren.id("tiger"), false);
    private static final GeoModel<TortoiseEntity> TORTOISE_MODEL =
            new DefaultedEntityGeoModel<>(Guzhenren.id("tortoise"), false);
    private static final GeoModel<CattleEntity> BUFFALO_MODEL =
            new DefaultedEntityGeoModel<>(Guzhenren.id("buffalo"), false);
    private static final GeoModel<ElephantEntity> ELEPHANT_MODEL =
            new DefaultedEntityGeoModel<>(Guzhenren.id("elephant"), false);
    private static final HumanSoulGeoModel HUMAN_SOUL_MODEL = new HumanSoulGeoModel();

    private static final ResourceLocation WHITE_BOAR_GU = getTexture("white_boar_gu");
    private static final ResourceLocation BLACK_BOAR_GU = getTexture("black_boar_gu");
    private static final ResourceLocation FLOWER_BOAR_GU = getTexture("flower_boar_gu");
    private static final ResourceLocation CRASH_GU_SILVER = getTexture("crash_gu");
    private static final ResourceLocation CRASH_GU_GOLD = getTexture("charging_crash_gu");
    private static final ResourceLocation CRASH_GU_AMETHYST = getTexture("charging_crash_gu_5");
    private static final ResourceLocation WILD_BOAR = getTexture("wild_boar");
    private static final ResourceLocation BROWN_BEAR = getTexture("brown_bear");
    private static final ResourceLocation ASIAN_BLACK_BEAR = getTexture("asian_black_bear");
    private static final ResourceLocation AMERICAN_BLACK_BEAR = getTexture("american_black_bear");
    private static final ResourceLocation ALBINO_BEAR = getTexture("albino_bear");
    private static final ResourceLocation ORANGE_TIGER = getTexture("tiger");
    private static final ResourceLocation WHITE_TIGER = getTexture("white_tiger");
    private static final ResourceLocation TORTOISE = getTexture("tortoise");
    private static final ResourceLocation STONE_TORTOISE = getTexture("stone_tortoise");
    private static final ResourceLocation YELLOW_OX = getTexture("yellow_ox");
    private static final ResourceLocation WATER_BUFFALO = getTexture("water_buffalo");
    private static final ResourceLocation AZURE_OX = getTexture("azure_ox");
    private static final ResourceLocation ELEPHANT = getTexture("elephant");
    private static final ResourceLocation WHITE_ELEPHANT = getTexture("white_elephant");

    public static void register(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(ModEntityTypes.HOPE_GU_ENTITY.get(), NoopRenderer::new);
        event.registerEntityRenderer(ModEntityTypes.WHITE_BOAR_GU_ENTITY.get(),
                context -> createBoarGuRenderer(context, WHITE_BOAR_GU));
        event.registerEntityRenderer(ModEntityTypes.BLACK_BOAR_GU_ENTITY.get(),
                context -> createBoarGuRenderer(context, BLACK_BOAR_GU));
        event.registerEntityRenderer(ModEntityTypes.FLOWER_BOAR_GU_ENTITY.get(),
                context -> createBoarGuRenderer(context, FLOWER_BOAR_GU));
        event.registerEntityRenderer(ModEntityTypes.HORIZONTAL_CRASH_GU_ENTITY.get(),
                context -> createCrashGuRenderer(context, CRASH_GU_SILVER));
        event.registerEntityRenderer(ModEntityTypes.VERTICAL_CRASH_GU_ENTITY.get(),
                context -> createCrashGuRenderer(context, CRASH_GU_SILVER));
        event.registerEntityRenderer(ModEntityTypes.CHARGING_CRASH_GU_4_ENTITY.get(),
                context -> createCrashGuRenderer(context, CRASH_GU_GOLD));
        event.registerEntityRenderer(ModEntityTypes.CHARGING_CRASH_GU_5_ENTITY.get(),
                context -> createCrashGuRenderer(context, CRASH_GU_AMETHYST));
        event.registerEntityRenderer(ModEntityTypes.WILD_BOAR.get(),
                context -> new FixedTextureGeoRenderer<>(context, WILD_BOAR_MODEL, WILD_BOAR, 0.55F, false));
        event.registerEntityRenderer(ModEntityTypes.BROWN_BEAR.get(),
                context -> createBearRenderer(context, BROWN_BEAR));
        event.registerEntityRenderer(ModEntityTypes.ASIAN_BLACK_BEAR.get(),
                context -> createBearRenderer(context, ASIAN_BLACK_BEAR));
        event.registerEntityRenderer(ModEntityTypes.AMERICAN_BLACK_BEAR.get(),
                context -> createBearRenderer(context, AMERICAN_BLACK_BEAR));
        event.registerEntityRenderer(ModEntityTypes.ALBINO_BEAR.get(),
                context -> createBearRenderer(context, ALBINO_BEAR));
        event.registerEntityRenderer(ModEntityTypes.TIGER.get(), context -> createTigerRenderer(context, ORANGE_TIGER));
        event.registerEntityRenderer(ModEntityTypes.WHITE_TIGER.get(),
                context -> createTigerRenderer(context, WHITE_TIGER));
        event.registerEntityRenderer(ModEntityTypes.TORTOISE.get(),
                context -> createTortoiseRenderer(context, TORTOISE));
        event.registerEntityRenderer(ModEntityTypes.STONE_TORTOISE.get(),
                context -> createTortoiseRenderer(context, STONE_TORTOISE));
        event.registerEntityRenderer(ModEntityTypes.YELLOW_OX.get(), context -> createCattleRenderer(context, YELLOW_OX));
        event.registerEntityRenderer(ModEntityTypes.WATER_BUFFALO.get(),
                context -> createCattleRenderer(context, WATER_BUFFALO));
        event.registerEntityRenderer(ModEntityTypes.AZURE_OX.get(), context -> createCattleRenderer(context, AZURE_OX));
        event.registerEntityRenderer(ModEntityTypes.ELEPHANT.get(), context -> createElephantRenderer(context, ELEPHANT));
        event.registerEntityRenderer(ModEntityTypes.WHITE_ELEPHANT.get(),
                context -> createElephantRenderer(context, WHITE_ELEPHANT));
        event.registerEntityRenderer(ModEntityTypes.BEAST_PHANTOM.get(), BeastPhantomRenderer::new);
        event.registerEntityRenderer(ModEntityTypes.TEST_TRADE_GU_IMMORTAL.get(),
                context -> new SoulTraderGeoRenderer(context, HUMAN_SOUL_MODEL, SoulTraderGeoRenderer.BLUE_TEXTURE));
        event.registerEntityRenderer(ModEntityTypes.MOON_BLADE.get(), MoonBladeRenderer::new);
        event.registerEntityRenderer(ModEntityTypes.QI_WAVE.get(), NoopRenderer::new);
        event.registerEntityRenderer(ModEntityTypes.BEAST_CHARGE.get(), BeastPhantomRenderer::new);
    }

    private static EntityRenderer<BoarGuEntity> createBoarGuRenderer(EntityRendererProvider.Context context,
                                                                     ResourceLocation texture) {
        return new FixedTextureGeoRenderer<>(context, BOAR_GU_MODEL, texture, 0.2F, true).withScale(BOAR_GU_SCALE);
    }

    private static EntityRenderer<CrashGuEntity> createCrashGuRenderer(EntityRendererProvider.Context context,
                                                                       ResourceLocation texture) {
        return new FixedTextureGeoRenderer<>(context, CRASH_GU_MODEL, texture, 0.2F, false);
    }

    private static EntityRenderer<BearEntity> createBearRenderer(EntityRendererProvider.Context context,
                                                                 ResourceLocation texture) {
        return new FixedTextureGeoRenderer<>(context, BEAR_MODEL, texture, 0.7F, false);
    }

    private static EntityRenderer<TigerEntity> createTigerRenderer(EntityRendererProvider.Context context,
                                                                   ResourceLocation texture) {
        return new FixedTextureGeoRenderer<>(context, TIGER_MODEL, texture, 0.6F, false);
    }

    private static EntityRenderer<TortoiseEntity> createTortoiseRenderer(EntityRendererProvider.Context context,
                                                                         ResourceLocation texture) {
        return new FixedTextureGeoRenderer<>(context, TORTOISE_MODEL, texture, 0.6F, false)
                .withMotionAnimThreshold(TortoiseEntity.WALK_ANIM_THRESHOLD);
    }

    private static EntityRenderer<CattleEntity> createCattleRenderer(EntityRendererProvider.Context context,
                                                                     ResourceLocation texture) {
        return new FixedTextureGeoRenderer<>(context, BUFFALO_MODEL, texture, 0.9F, false);
    }

    private static EntityRenderer<ElephantEntity> createElephantRenderer(EntityRendererProvider.Context context,
                                                                         ResourceLocation texture) {
        return new FixedTextureGeoRenderer<>(context, ELEPHANT_MODEL, texture, 1.3F, false).withScale(ELEPHANT_SCALE);
    }

    private static ResourceLocation getTexture(String name) {
        return Guzhenren.id("textures/entity/" + name + ".png");
    }
}
