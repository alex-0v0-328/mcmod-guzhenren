package net.alex.guzhenren.client.renderer;

import net.alex.guzhenren.Guzhenren;
import net.alex.guzhenren.entity.BearEntity;
import net.alex.guzhenren.entity.BoarGuEntity;
import net.alex.guzhenren.entity.CrashGuEntity;
import net.alex.guzhenren.entity.TigerEntity;
import net.alex.guzhenren.entity.WildBoarEntity;
import net.alex.guzhenren.registry.entity.ModEntityTypes;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.NoopRenderer;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import software.bernie.geckolib.model.DefaultedEntityGeoModel;
import software.bernie.geckolib.model.GeoModel;

/**
 * Every entity renderer this mod registers: each family's shared GeckoLib model with one fixed texture per
 * entity type, the soul traders' translucent {@code human_soul}, and the Hope Gu [希望蛊] as a
 * {@link NoopRenderer} (pure particles, no model).
 *
 * <p>The boar Gu's ladybug geometry is life-sized (about 0.19 by 0.28 blocks); {@link #BOAR_GU_SCALE} draws it
 * at the width of the 0.4-block hitbox without touching the exported geometry, and it keeps the vanilla death
 * tilt. Every other family turns the tilt off because its death animation lies the body down by itself. The
 * crash Gu textures follow rank: dark silver for both rank-three Gu (one shared file), dark gold for rank four
 * and dark amethyst for rank five.
 *
 * @author Alex
 * @version 1.0.0
 * @since 1.0.0
 */

public final class ModEntityRenderers {

    private ModEntityRenderers() {}

    private static final float BOAR_GU_SCALE = 2.0F;
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
    private static final HumanSoulGeoModel HUMAN_SOUL_MODEL = new HumanSoulGeoModel();

    private static final ResourceLocation WHITE_BOAR_GU = texture("white_boar_gu");
    private static final ResourceLocation BLACK_BOAR_GU = texture("black_boar_gu");
    private static final ResourceLocation FLOWER_BOAR_GU = texture("flower_boar_gu");
    private static final ResourceLocation CRASH_GU_SILVER = texture("crash_gu");
    private static final ResourceLocation CRASH_GU_GOLD = texture("charging_crash_gu");
    private static final ResourceLocation CRASH_GU_AMETHYST = texture("charging_crash_gu_5");
    private static final ResourceLocation WILD_BOAR = texture("wild_boar");
    private static final ResourceLocation BROWN_BEAR = texture("brown_bear");
    private static final ResourceLocation ASIAN_BLACK_BEAR = texture("asian_black_bear");
    private static final ResourceLocation AMERICAN_BLACK_BEAR = texture("american_black_bear");
    private static final ResourceLocation ALBINO_BEAR = texture("albino_bear");
    private static final ResourceLocation ORANGE_TIGER = texture("tiger");
    private static final ResourceLocation WHITE_TIGER = texture("white_tiger");

    public static void register(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(ModEntityTypes.HOPE_GU_ENTITY.get(), NoopRenderer::new);
        event.registerEntityRenderer(ModEntityTypes.WHITE_BOAR_GU_ENTITY.get(),
                context -> boarGu(context, WHITE_BOAR_GU));
        event.registerEntityRenderer(ModEntityTypes.BLACK_BOAR_GU_ENTITY.get(),
                context -> boarGu(context, BLACK_BOAR_GU));
        event.registerEntityRenderer(ModEntityTypes.FLOWER_BOAR_GU_ENTITY.get(),
                context -> boarGu(context, FLOWER_BOAR_GU));
        event.registerEntityRenderer(ModEntityTypes.HORIZONTAL_CRASH_GU_ENTITY.get(),
                context -> crashGu(context, CRASH_GU_SILVER));
        event.registerEntityRenderer(ModEntityTypes.VERTICAL_CRASH_GU_ENTITY.get(),
                context -> crashGu(context, CRASH_GU_SILVER));
        event.registerEntityRenderer(ModEntityTypes.CHARGING_CRASH_GU_4_ENTITY.get(),
                context -> crashGu(context, CRASH_GU_GOLD));
        event.registerEntityRenderer(ModEntityTypes.CHARGING_CRASH_GU_5_ENTITY.get(),
                context -> crashGu(context, CRASH_GU_AMETHYST));
        event.registerEntityRenderer(ModEntityTypes.WILD_BOAR.get(),
                context -> new FixedTextureGeoRenderer<>(context, WILD_BOAR_MODEL, WILD_BOAR, 0.55F, false));
        event.registerEntityRenderer(ModEntityTypes.BROWN_BEAR.get(), context -> bear(context, BROWN_BEAR));
        event.registerEntityRenderer(ModEntityTypes.ASIAN_BLACK_BEAR.get(), context -> bear(context, ASIAN_BLACK_BEAR));
        event.registerEntityRenderer(ModEntityTypes.AMERICAN_BLACK_BEAR.get(),
                context -> bear(context, AMERICAN_BLACK_BEAR));
        event.registerEntityRenderer(ModEntityTypes.ALBINO_BEAR.get(), context -> bear(context, ALBINO_BEAR));
        event.registerEntityRenderer(ModEntityTypes.TIGER.get(), context -> tiger(context, ORANGE_TIGER));
        event.registerEntityRenderer(ModEntityTypes.WHITE_TIGER.get(), context -> tiger(context, WHITE_TIGER));
        event.registerEntityRenderer(ModEntityTypes.TEST_TRADE_GU_IMMORTAL.get(),
                context -> new SoulTraderGeoRenderer(context, HUMAN_SOUL_MODEL, SoulTraderGeoRenderer.BLUE_TEXTURE));
    }

    private static EntityRenderer<BoarGuEntity> boarGu(EntityRendererProvider.Context context,
                                                       ResourceLocation texture) {
        return new FixedTextureGeoRenderer<>(context, BOAR_GU_MODEL, texture, 0.2F, true).withScale(BOAR_GU_SCALE);
    }

    private static EntityRenderer<CrashGuEntity> crashGu(EntityRendererProvider.Context context,
                                                         ResourceLocation texture) {
        return new FixedTextureGeoRenderer<>(context, CRASH_GU_MODEL, texture, 0.2F, false);
    }

    private static EntityRenderer<BearEntity> bear(EntityRendererProvider.Context context, ResourceLocation texture) {
        return new FixedTextureGeoRenderer<>(context, BEAR_MODEL, texture, 0.7F, false);
    }

    private static EntityRenderer<TigerEntity> tiger(EntityRendererProvider.Context context, ResourceLocation texture) {
        return new FixedTextureGeoRenderer<>(context, TIGER_MODEL, texture, 0.6F, false);
    }

    private static ResourceLocation texture(String name) {
        return Guzhenren.id("textures/entity/" + name + ".png");
    }
}
